package org.jetlinks.project.busi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.annotation.*;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.pro.io.excel.ExcelUtils;
import org.jetlinks.pro.io.excel.ImportHelper;
import org.jetlinks.pro.io.file.FileInfo;
import org.jetlinks.pro.io.file.FileManager;
import org.jetlinks.pro.io.file.FileOption;
import org.jetlinks.pro.io.utils.FileUtils;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.AreaInfoEntity;
import org.jetlinks.project.busi.entity.ChangeRecordEntity;
import org.jetlinks.project.busi.entity.DeviceInfoEntity;
//import org.jetlinks.project.busi.iot.IotService;
import org.jetlinks.project.busi.service.AreaInfoService;
import org.jetlinks.project.busi.service.ChangeRecordService;
import org.jetlinks.project.busi.service.DeviceService;
import org.jetlinks.project.busi.utils.SnowflakeIdWorker;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/area/info")
@AllArgsConstructor
@Getter
@Tag(name = "1.0 区域信息") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "area-info", name = "区域信息")
public class AreaInfoController implements AssetsHolderCrudController<AreaInfoEntity,String> {

    private final AreaInfoService service;

    private final QueryHelper queryHelper;

    private final FileManager fileManager;

    private final ReactiveRedisTemplate<String, String> redis;

    private final DeviceService deviceService;

//    private final IotService iotService;

    private final ChangeRecordService changeRecordService;

    //根据上传的文件来导入数据并将导入结果保存到文件中返回结果文件地址，
    //客户端可以引导用户下载结果文件
    @PostMapping("/_import.{format}")
    @Operation(summary = "导入")
    @ResourceAction(id= Permission.ACTION_IMPORT,name = "导入")
    public Mono<String> importByFileUpload(@PathVariable String format,
                                           @RequestPart("file") Mono<FilePart> file) {


        return FileUtils
            .dataBufferToInputStream(file.flatMapMany(FilePart::content))
            .flatMap(inputstream -> new ImportHelper<>(
                AreaInfoEntity::new,
                //数据处理逻辑
                flux -> service.save(flux).then())
                //批量处理数量
                .bufferSize(200)
                //当批量处理失败时,是否回退到单条数据处理
                .fallbackSingle(true)
                .doImport(inputstream,
                    format,
                    //忽略导入结果
                    info -> null,
                    //将导入的结果保存为临时文件
                    result -> fileManager
                        .saveFile("import." + format, result, FileOption.tempFile)
                        .map(FileInfo::getAccessUrl))
                .last()
            );
    }

    @GetMapping("/getName")
    @Operation(summary = "通过区域id获取redis的名称")
    @QueryAction
    public Mono<String> getAreaNameById(String id) {

        return redis.hasKey(id)
            .flatMap(l -> {
                if (l) {
                    return redis.opsForValue().get(id);
                }
                return service
                    .findById(id)
                    .map(AreaInfoEntity::getAreaName)
                    .doOnNext(Mono::just);
            });
    }


    //导出模板
    @GetMapping("/_export")
    @Operation(summary = "导出模板")
    @ResourceAction(id= Permission.ACTION_EXPORT,name = "导出")
    public Mono<Void> export(String format,
                             ServerWebExchange exchange) {

        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_OCTET_STREAM);
        //文件名
        exchange.getResponse().getHeaders().setContentDisposition(
            ContentDisposition
                .attachment()
                .filename("模板", StandardCharsets.UTF_8)
                .build()
        );
        return exchange
            .getResponse()
            .writeWith(
                ExcelUtils.write(AreaInfoEntity.class, Flux.empty(), format)
            );
    }

    @Operation(summary = "获取区域树")
    @GetMapping("/get/tree")
    @QueryAction
    public Mono<List<AreaInfoEntity>> getRegionTRee() {
        return service
            .createQuery()
            .where(AreaInfoEntity::getState, "0")
            .fetch()
            .collectList()
            .flatMap(areaInfoList -> Mono.just(createTree("0", areaInfoList)));
    }

    /**
     * 递归生成树
     */
    public static List<AreaInfoEntity> createTree(String pCode, List<AreaInfoEntity> areaInfos) {
        List<AreaInfoEntity> treeMenu = new ArrayList<>();

        for (AreaInfoEntity areaInfo : areaInfos) {

            if (pCode.equals(areaInfo.getParentId())) {
                treeMenu.add(areaInfo);
                areaInfo.setChildren(createTree(areaInfo.getId(), areaInfos));
            }
        }
        return treeMenu;
    }

    /**
     * 递归查询区域列表
     */
    public Mono<List<AreaInfoEntity>> getAreaInfos(List<AreaInfoEntity> areaInfos, List<AreaInfoEntity> resList) {
        return service
            .createQuery()
            .in(AreaInfoEntity::getId, areaInfos.stream().map(AreaInfoEntity::getParentId).distinct().collect(Collectors.toList()))
            .fetch()
            .collectList()
            .flatMap(areaInfoList -> {
                if (ObjectUtils.isEmpty(areaInfoList)) {
                    return Mono.just(resList);
                }
                resList.addAll(areaInfoList);
                return getAreaInfos(areaInfoList, resList);
            });
    }

//    @PostMapping("/bind/unit/device")
//    @Operation(summary = "绑定设备")
//    public Mono<Long> bindDevice(@RequestBody AreaUnitDeviceReq areaUnitDeviceReq){
//        List<String> strings = Arrays.asList(areaUnitDeviceReq.getDeviceIds());
//        return Flux.fromIterable(strings)
//            .flatMap(value->{
//                AreaUnitDeviceEntity areaUnitDeviceEntity = new AreaUnitDeviceEntity();
//                areaUnitDeviceEntity.setAreaId(areaUnitDeviceReq.getAreaId());
//                areaUnitDeviceEntity.setDeviceId(value);
//                return areaUnitDeviceService.insert(areaUnitDeviceEntity);
//            }).count();
//    }


    @PostMapping("/treeList")
    @Operation(summary = "列表")
    @QueryAction
    public Flux<AreaInfoEntity> getAllTree(@RequestBody QueryParamEntity entity) {
        entity.setParallelPager(false);
        entity.setPaging(false);
        return service
            .query(entity)
            .collectList()
            .flatMapIterable(list -> TreeSupportEntity.list2tree(list, AreaInfoEntity::setChildren));
    }

//    @Operation(summary = "新增区域扩展")
//    @PostMapping("/insert/extend")
//    @Transactional
//    @SaveAction
//    public Mono<Object> insertRegion(@RequestBody AreaInfoEntity areaInfoEntity) {
//        //判断是否绑定表
//        areaInfoEntity.setBindState("0");
//        if (!areaInfoEntity.getDeviceChildren().isEmpty() && areaInfoEntity.getDeviceChildren() != null) {
//            areaInfoEntity.setBindState("1");
//        }
//        return service
//            .insert(areaInfoEntity)
//            .thenReturn(areaInfoEntity)
//            .flatMap(i -> {
//                return Flux.fromIterable(areaInfoEntity.getDeviceChildren())
//                    .flatMap(value -> {
//                        value.setMeterNumber(iotService.getMeterNumber(value.getDeviceId(),value.getEnergyType()));
//                        value.setParentFlag("1");
//                        value.setParentId(i.getId());
//                        value.setAreaId(i.getId());
//                        return deviceService.insert(value);
//                    })
//                    .doOnError(throwable -> {
//                        throw new RuntimeException("设备表数不存在，请初始化表数");
//                    })
//                    .then();
//            });
//
//    }

//    @Operation(summary = "编辑区域扩展")
//    @PostMapping("/edit/extend")
//    @ResourceAction(id= Permission.ACTION_UPDATE, name = "更新")
//    public Mono<Object> editRegion(@RequestBody AreaInfoEntity areaInfoEntity) {
//        Collection<DeviceInfoEntity> insertEntity = new ArrayList<>();
//        //判断是否绑定表
//        areaInfoEntity.setBindState("0");
//        if (!areaInfoEntity.getDeviceChildren().isEmpty() && areaInfoEntity.getDeviceChildren() != null) {
//            areaInfoEntity.setBindState("1");
//        }
//        return Mono.just(areaInfoEntity)
//            .flatMap(
//                //1.查询区域关联的所有水电气表
//                value -> deviceService.createQuery().where(DeviceInfoEntity::getParentId, value.getId())
//                    .where(DeviceInfoEntity::getStatus, "0")
//                    .fetch()
//                    .collectMap(deviceInfoEntity -> deviceInfoEntity.getEnergyType()[0].getValue(), deviceInfoEntity -> deviceInfoEntity)
//                    .flatMapMany(oldChilds -> {
//                        //2.遍历，如果传入的子设备存在能源类型相同，但与之前的设备名不同,设置新子设备，并更新old字段
//                        List<DeviceInfoEntity> newChilds = value.getDeviceChildren();
//
//                        //用于存放新的能源类型
//                        ArrayList<String> newType = new ArrayList<>();
//                        //3.传入的子设备遍历关联的子设备,修改情况
//                        for (DeviceInfoEntity newChildInfo : newChilds) {
//                            String newEnergyType = newChildInfo.getEnergyType()[0].getValue();
//                            newType.add(newEnergyType);
//                            //3.1 如果有数据
//                            if (oldChilds.get(newEnergyType) != null) {
//                                DeviceInfoEntity deviceInfoEntity = oldChilds.get(newEnergyType);
//                                if (!deviceInfoEntity.getDeviceId().equals(newChildInfo.getDeviceId())) {
//                                    //3.1.1 更换状态
//                                    deviceInfoEntity.setStatus("2");
//                                    oldChilds.replace(newEnergyType, deviceInfoEntity);
//                                    //3.1.2 另加数据
//                                    DeviceInfoEntity insertChild = new DeviceInfoEntity();
//                                    BeanUtils.copyProperties(deviceInfoEntity, insertChild);
//                                    String id = String.valueOf(new SnowflakeIdWorker().nextId());
//                                    insertChild.setId(id);
//                                    insertChild.setParentFlag("1");
//                                    insertChild.setDeviceId(newChildInfo.getDeviceId());
//                                    insertChild.setAreaId(areaInfoEntity.getId());
//                                    insertChild.setDeviceName(newChildInfo.getDeviceName());
//                                    insertChild.setMeterNumber(iotService.getMeterNumber(newChildInfo.getDeviceId(),newChildInfo.getEnergyType()));
//                                    insertChild.setStatus("0");
//                                    insertEntity.add(insertChild);
//                                }
////
//                            } //3.2如果没有数据就新增数据
//                            else {
//                                newChildInfo.setId(null);
//                                newChildInfo.setParentId(areaInfoEntity.getId());
//                                newChildInfo.setAreaId(areaInfoEntity.getId());
//                                newChildInfo.setStatus("0");
//                                newChildInfo.setParentFlag("1");
//                                newChildInfo.setMeterNumber(iotService.getMeterNumber(newChildInfo.getDeviceId(),newChildInfo.getEnergyType()));
//                                oldChilds.put(newEnergyType, newChildInfo);
//                            }
//                        }
//                        //判断是否减少了能源类型
//                        Set<String> keySet = oldChilds.keySet();
//                        ArrayList<String> strings = new ArrayList<>();
//                        for (String s : keySet) {
//                            if (!newType.contains(s)) {
//                                strings.add(s);
//                            }
//                        }
//                        if (!strings.isEmpty()) {
//                            //获取减少的能源类型
//                            for (String s : strings) {
//                                //把该能源类型绑定的状态为0的表变为1
//                                DeviceInfoEntity deviceInfo = oldChilds.get(s);
//                                deviceInfo.setStatus("1");
//                            }
//                        }
//                        return Flux.fromIterable(oldChilds.values());
//                    }).collectList()
//            )
//            .doOnError(throwable -> {
//                throw new RuntimeException("设备表数不存在，请初始化表数");
//            })
//            .flatMap(child -> service.save(areaInfoEntity)
//                .then(deviceService.save(insertEntity))
//                .then(deviceService.save(child))
//                .then(changeRecordService.save(areaInfoEntity.getChangeRecordEntity() == null ? new ArrayList<ChangeRecordEntity>() : areaInfoEntity.getChangeRecordEntity())));
//    }


    @Operation(summary = "校验设备Id是否唯一")
    @PostMapping("/check/only")
    @Authorize(ignore = true)
    public Mono<Object> CheckOnly(@RequestBody DeviceInfoEntity entity) {
        return deviceService
            .createQuery()
            .where(DeviceInfoEntity::getDeviceId, entity.getDeviceId())
            .where(DeviceInfoEntity::getParentFlag, "1")
            .and(DeviceInfoEntity::getStatus, "eq", "0")
            .fetch()
            .collectList()
            .flatMap(v -> {
                if (v.size() == 0) {
                    //判断该表是否已经绑定设备
                    return deviceService
                        .createQuery()
                        .where(DeviceInfoEntity::getDeviceId, entity.getDeviceId())
                        .where(DeviceInfoEntity::getParentFlag, "0")
                        .not(DeviceInfoEntity::getStatus,"1")
                        .fetch()
                        .collectList()
                        .hasElement()
                        .flatMap(aBoolean -> {
                            if(aBoolean){
                                return Mono.just(1);
                            }
                            return Mono.just(0);
                        });
                }
                return Mono.just(1);
            });
    }


    @Operation(summary = "删除扩展")
    @DeleteMapping("/del/extend/{areaId}")
    @DeleteAction
    public Mono<Integer> deleteExtend(@PathVariable("areaId") String areaId) {
        return service
            .createUpdate()
            .set(AreaInfoEntity::getState, "1")
            .where(AreaInfoEntity::getId, areaId)
            .execute()
            .flatMap(e -> {
                if (e > 0) {
                    return
                        deviceService
                            .createUpdate()
                            .set(DeviceInfoEntity::getStatus, "1")
                            .where(DeviceInfoEntity::getParentId, areaId)
                            .execute();
                } else {
                    return Mono.empty();
                }
            });

    }


    @Operation(summary = "编辑回显")
    @PostMapping("/before/edit/{areaId}")
    @QueryAction
    public Mono<AreaInfoEntity> editRetrun(@PathVariable("areaId") String areaId) {
        return service.findById(areaId)
            .flatMap(value -> {
                return deviceService
                    .createQuery()
                    .where(DeviceInfoEntity::getParentId, areaId)
                    .where(DeviceInfoEntity::getStatus, "0")
                    .fetch()
                    .collectList()
                    .doOnNext(list -> value.setDeviceChildren(list))
                    .thenReturn(value);
            });

    }




    @Operation(summary = "判断是否绑定表")
    @PostMapping("/deterMine")
    @Authorize(ignore = true)
    public Mono<Boolean> determine(@RequestBody AreaInfoEntity areaInfo) {

        if(areaInfo.getParentId() !=null){
            //判断该区域是否绑定表
            return service
                .findById(areaInfo.getId())
                .flatMap(value->{
                    if("1".equals(value.getBindState()) && !areaInfo.getParentId().equals(value.getParentId())){
                        return Mono.just(Boolean.TRUE);
                    }else {
                        //判断该区域下是否有设备
                        return deviceService
                            .createQuery()
                            .where(DeviceInfoEntity::getAreaId,areaInfo.getId())
                            .not(DeviceInfoEntity::getStatus,"1")
                            .where(DeviceInfoEntity::getParentId,"0")
                            .fetch()
                            .hasElements()
                            .flatMap(aBoolean -> {
                                if(aBoolean){
                                    return Mono.just(Boolean.TRUE);
                                }
                                return Mono.just(Boolean.FALSE);
                            });
                    }
                });
    }
        return Mono.just(Boolean.FALSE);
}







}
