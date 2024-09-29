package org.jetlinks.project.busi.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.utils.StringUtils;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.annotation.*;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.*;
import org.jetlinks.project.busi.entity.req.IotReq;
import org.jetlinks.project.busi.entity.res.DeviceIotRes;
import org.jetlinks.project.busi.service.*;
//import org.jetlinks.project.busi.iot.IotService;
import org.jetlinks.project.busi.utils.SnowflakeIdWorker;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/sems/device")
@AllArgsConstructor
@Getter
@Tag(name = "设备信息1.0") //swagger
@Slf4j
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "device", name = "设备信息")
public class DeviceController implements AssetsHolderCrudController<DeviceInfoEntity,String> {

    private final DeviceService service;

    private final QueryHelper queryHelper;

//    private final IotService iotService;

    private final CombinationUnitDeviceService unitDeviceService;

    private final TestConfigDeviceService testConfigDeviceService;

    private final ChangeRecordService changeRecordService;

    private final HitDotService hitDotService;

    private final OperateLogService operateLogService;

//    @Operation(summary = "新增数据2.0")
//    @PostMapping("_insert/new")
//    @SaveAction
//    public Mono<SaveResult> insertNew(@RequestBody DeviceInfoEntity entity) {
//        String id = String.valueOf(new SnowflakeIdWorker().nextId());
//        return Mono.just(entity)
//            .doOnNext(value -> value.setId(id))
//            .flatMap(value ->
//                Flux.fromIterable(value.getChild())
//                    .doOnNext(child ->  {
//                        child.setMeterNumber(iotService.getMeterNumber(child.getDeviceId(),child.getEnergyType()));
//                        child.setParentId(entity.getDeviceId());
//                    })
//                    .doOnError(throwable ->{throw new RuntimeException("设备表数不存在，请初始化表数");})
//                    .collectList()
//                    .flatMap(child -> service.insert(value)
//                                             .then(service.save(child))));
//    }


    @Operation(summary = "批量修改数据")
    @PostMapping("_update/batch")
    @ResourceAction(id= Permission.ACTION_UPDATE, name = "更新")
    public Mono<SaveResult> updateBatch(@RequestBody DeviceInfoEntity[] deviceInfoEntities) {
        Collection<DeviceInfoEntity> collection = Arrays.asList(deviceInfoEntities);
        return service
            .save(collection);
    }

    @Operation(summary = "查询数据2.0")
    @PostMapping("_query/new")
    @QueryAction
    public Flux<Object> queryNew(@RequestBody DeviceInfoEntity entity) {
       return service
            .createQuery()
            .where(DeviceInfoEntity::getId,entity.getId())
            .fetch()
            .flatMap(data -> service
                .createQuery()
                .where(DeviceInfoEntity::getParentId,entity.getDeviceId())
                .and(DeviceInfoEntity::getStatus,"eq","0")
                .fetch()
                .collectList()
                .map(m -> {
                    data.setChild(m);
                    return data;
                }));
    }

//    @Operation(summary = "编辑数据2.0")
//    @PostMapping("_update/new")
//    @ResourceAction(id= Permission.ACTION_UPDATE, name = "更新")
//    public Mono<SaveResult> updateNew(@RequestBody DeviceInfoEntity entity) {
//        Collection<DeviceInfoEntity> insertEntity = new ArrayList<>();
//        return Mono.just(entity)
//                   .flatMap(
//                       //1.查询关联的所有水电气表
//                       value -> service.createQuery().where(DeviceInfoEntity::getParentId,value.getDeviceId())
//                                                    .where(DeviceInfoEntity::getStatus,"0")
//                                                     .fetch()
//                                                     .collectMap(deviceInfoEntity -> deviceInfoEntity.getEnergyType()[0].getValue(),deviceInfoEntity -> deviceInfoEntity)
//                                                     .flatMapMany(oldChilds -> {
//                                                         //2.遍历，如果传入的子设备存在能源类型相同，但与之前的设备名不同,设置新子设备，并更新old字段
//                                                         List<DeviceInfoEntity> newChilds = value.getChild();
//                                                         //用于存放新的能源类型
//                                                         ArrayList<String> newType = new ArrayList<>();
//
//                                                         //3.传入的子设备遍历关联的子设备,修改情况
//                                                         for (DeviceInfoEntity newChildInfo : newChilds) {
//                                                             String newEnergyType = newChildInfo.getEnergyType()[0].getValue();
//                                                             newType.add(newEnergyType);
//                                                             //3.1 如果有数据
//                                                             if(oldChilds.get(newEnergyType) != null){
//                                                                 DeviceInfoEntity deviceInfoEntity = oldChilds.get(newEnergyType);
//                                                                 if(!deviceInfoEntity.getDeviceId().equals(newChildInfo.getDeviceId())){
//                                                                     //3.1.1 更换状态
//                                                                     deviceInfoEntity.setStatus("2");
//                                                                     oldChilds.replace(newEnergyType,deviceInfoEntity);
//                                                                     //3.1.2 另加数据
//                                                                     DeviceInfoEntity insertChild = new DeviceInfoEntity();
//                                                                     BeanUtils.copyProperties(deviceInfoEntity,insertChild);
//                                                                     String id = String.valueOf(new SnowflakeIdWorker().nextId());
//                                                                     insertChild.setId(id);
//                                                                     insertChild.setDeviceId(newChildInfo.getDeviceId());
//                                                                     insertChild.setDeviceName(newChildInfo.getDeviceName());
//                                                                     insertChild.setMeterNumber(iotService.getMeterNumber(newChildInfo.getDeviceId(),newChildInfo.getEnergyType()));
//                                                                     insertChild.setStatus("0");
//                                                                     insertEntity.add(insertChild);
//                                                                 }else {
//                                                                     //判断区域是否更改
//                                                                     if(!deviceInfoEntity.getAreaId().equals(newChildInfo.getAreaId())){
//                                                                         return Mono.error(new UnsupportedOperationException("区域变更，该设备绑定的表不能是之前的表！"));
//                                                                     }
//                                                                 }
////
//                                                             } //3.2如果没有数据就新增数据
//                                                             else {
//                                                                 newChildInfo.setId(null);
//                                                                 newChildInfo.setParentId(entity.getDeviceId());
//                                                                 newChildInfo.setMeterNumber(iotService.getMeterNumber(newChildInfo.getDeviceId(),newChildInfo.getEnergyType()));
//                                                                 oldChilds.put(newEnergyType,newChildInfo);
//                                                             }
//                                                             //判断是否减少了能源类型
//                                                             Set<String> keySet = oldChilds.keySet();
//                                                             ArrayList<String> strings = new ArrayList<>();
//                                                             for (String s : keySet) {
//                                                                 if(!newType.contains(s)){
//                                                                     strings.add(s);
//                                                                 }
//                                                             }
//                                                             if(!strings.isEmpty()){
//                                                                 //获取减少的能源类型
//                                                                 for (String s : strings) {
//                                                                     //把该能源类型绑定的状态为0的表变为1
//                                                                     DeviceInfoEntity deviceInfo = oldChilds.get(s);
//                                                                     deviceInfo.setStatus("1");
//                                                                 }
//                                                             }
//                                                         }
//                                                         return Flux.fromIterable(oldChilds.values());
//                                                     }).collectList()
//                   )
//                   .doOnError(throwable ->{throw new RuntimeException("设备表数不存在，请初始化表数");})
//                   .flatMap(child -> service.updateById(entity.getId(), entity)
//                 .then(service.save(insertEntity))
//                       .then(service.save(child))
//                        .then(changeRecordService.save(entity.getChangeRecordEntity()==null?new ArrayList<ChangeRecordEntity>():entity.getChangeRecordEntity())));
//        }


//    @Operation(summary = "编辑数据3.0")
//    @PostMapping("_update/new3")
//    @ResourceAction(id= Permission.ACTION_UPDATE, name = "更新")
//    public Mono<SaveResult> updateNew4(@RequestBody DeviceInfoEntity entity) {
//        ArrayList<DeviceInfoEntity> insertDevice = new ArrayList<>();
//        ArrayList<OperateLogEntity> operateLogList = new ArrayList<>();
//        return Authentication
//            .currentReactive()
//            .switchIfEmpty(Mono.error(UnAuthorizedException::new))//如果没有用户信息则抛出异常
//            .flatMap(autz-> Mono.just(entity).flatMap(
//                //1.查询关联的所有水电气表
//                value -> service
//                    .createQuery()
//                    .where(DeviceInfoEntity::getParentId,value.getDeviceId())
//                    .where(DeviceInfoEntity::getStatus,"0")
//                    .fetch()
//                    .collectList()
//                    .flatMap(oldChilds -> {
//                        //旧设备id
//                        List<String> oldDeviceList = oldChilds.stream().map(DeviceInfoEntity::getDeviceId).collect(Collectors.toList());
//                        //新设备id
//                        List<String> newDeviceList = entity.getChild().stream().map(DeviceInfoEntity::getDeviceId).collect(Collectors.toList());
//
//                        Map<String,String> oldDeviceMap = oldChilds.stream()
//                                                                   .collect(Collectors.toMap(old ->old.getEnergyType()[0].getText(), DeviceInfoEntity::getDeviceId,(key1 , key2)-> key1+","+key2 ));
//
//                        Map<String,String> newDeviceMap = entity.getChild()
//                                                                .stream()
//                                                                .collect(Collectors.toMap(newDevice ->newDevice.getEnergyType()[0].getText(), DeviceInfoEntity::getDeviceId,(key1 , key2)-> key1+","+key2 ));
//
//                        List<String> energyList = new ArrayList<>();
//                        energyList.add("水");
//                        energyList.add("电");
//                        energyList.add("气");
//                        for (String energyKey:energyList) {
//                            if(oldDeviceMap.containsKey(energyKey) && newDeviceMap.containsKey(energyKey)){
//                                if(!oldDeviceMap.get(energyKey).equals(newDeviceMap.get(energyKey))){
//                                    OperateLogEntity logEntity = new OperateLogEntity();
//                                    logEntity.setFuncModule("设备信息");
//                                    logEntity.setJobNumber(autz.getUser().getUsername());
//                                    logEntity.setOperateUser(autz.getUser().getName());
//                                    logEntity.setOperateContent("设备："+entity.getDeviceName()+"("+entity.getDeviceId()+")- 修改 -" +
//                                                                    energyKey +"表:"+ oldDeviceMap.get(energyKey) +"变更为:" + newDeviceMap.get(energyKey));
//                                    operateLogList.add(logEntity);
//                                }
//                            }
//                            if(!oldDeviceMap.containsKey(energyKey) && newDeviceMap.containsKey(energyKey)){
//                                OperateLogEntity logEntity = new OperateLogEntity();
//                                logEntity.setFuncModule("设备信息");
//                                logEntity.setJobNumber(autz.getUser().getUsername());
//                                logEntity.setOperateUser(autz.getUser().getName());
//                                logEntity.setOperateContent("设备："+entity.getDeviceName()+"("+entity.getDeviceId()+")- 新增 -" +
//                                                                energyKey + "表:" + newDeviceMap.get(energyKey));
//                                operateLogList.add(logEntity);
//                            }
//                            if(oldDeviceMap.containsKey(energyKey) && !newDeviceMap.containsKey(energyKey)){
//                                OperateLogEntity logEntity = new OperateLogEntity();
//                                logEntity.setFuncModule("设备信息");
//                                logEntity.setJobNumber(autz.getUser().getUsername());
//                                logEntity.setOperateUser(autz.getUser().getName());
//                                logEntity.setOperateContent("设备："+entity.getDeviceName()+"("+entity.getDeviceId()+")- 删除 -" +
//                                                                energyKey + "表:" + oldDeviceMap.get(energyKey));
//                                operateLogList.add(logEntity);
//                            }
//                        }
//
//                        //需要添加的设备
//                        ArrayList<String> insertEntity = new ArrayList<>();
//                        insertEntity.addAll(newDeviceList);
//                        insertEntity.removeAll(oldDeviceList);
//                        List<DeviceInfoEntity> collect = entity.getChild().stream().filter(i -> insertEntity.contains(i.getDeviceId())).collect(Collectors.toList());
//                        for (DeviceInfoEntity deviceInfo : collect) {
//                            deviceInfo.setParentId(entity.getDeviceId());
//                            deviceInfo.setMeterNumber(iotService.getMeterNumber(deviceInfo.getDeviceId(), deviceInfo.getEnergyType()));
//                        }
//                        insertDevice.addAll(collect);
//
//                        //需要删除的设备
//                        ArrayList<String> del = new ArrayList<>();
//                        del.addAll(oldDeviceList);
//                        del.removeAll(newDeviceList);
//                        List<DeviceInfoEntity> update = oldChilds.stream().filter(i -> del.contains(i.getDeviceId())).collect(Collectors.toList());
//                        for (DeviceInfoEntity deviceInfo : update) {
//                            deviceInfo.setStatus("2");
//                        }
//                        return Mono.just(update);
//                    })
//            ).doOnError(throwable ->{throw new RuntimeException("设备表数不存在，请初始化表数");})
//             .flatMap(child -> service
//                 .updateById(entity.getId(), entity)
//                 .then(service.save(insertDevice))
//                 .then(service.save(child))
//                 .then(changeRecordService.save(entity.getChangeRecordEntity()==null?new ArrayList<ChangeRecordEntity>():entity.getChangeRecordEntity()))
//                 .then(operateLogService.save(operateLogList))));
//    }


    /**
     * 保存
     * @param oldList
     * @param newList
     * @return
     */
    private List<String> saveList(List<String> oldList,List<String> newList){
        ArrayList<String> deviceInfoEntities = new ArrayList<>(newList);
        deviceInfoEntities.retainAll(oldList);
        return deviceInfoEntities;
    }

    /**
     * 删除
     * @param oldList
     * @param newList
     * @return
     */
    private List<String> deleteList(List<String> oldList,List<String> newList){
        ArrayList<String> deviceInfoEntities = new ArrayList<>(newList);
        deviceInfoEntities.removeAll(oldList);
        return deviceInfoEntities;
    }

    @Operation(summary = "删除数据2.0")
    @PostMapping("_delete/new")
    @DeleteAction
    public Mono<Integer> deleteNew(@RequestBody DeviceInfoEntity entity) {
        return service
            .createUpdate()
            .set(DeviceInfoEntity::getStatus,"1")
            .where(DeviceInfoEntity::getId,entity.getId())
            .execute()
            .flatMap(e -> {
                if(e > 0){
                    return service
                        .createUpdate()
                        .set(DeviceInfoEntity::getStatus,"1")
                        .where(DeviceInfoEntity::getParentId,entity.getDeviceId())
                        .execute();
                }else {
                    return Mono.empty();
                }
            })
            .then(testConfigDeviceService
                      .createDelete()
                      .where(TestConfigDeviceEntity::getDeviceId, entity.getDeviceId())
                      .execute())
            .then(hitDotService
                      .createDelete()
                      .where(HitDotEntity::getHouseDeviceId, entity.getDeviceId())
                      .execute());
    }

    /**
     * 从iot获取设备列表
     */
//    @Operation(summary = "从iot获取设备列表")
//    @PostMapping("iot/device/ids")
//    @ResourceAction(id=Permission.ACTION_GET,name = "获取iot设备")
//    public Flux<DeviceIotRes> getDeviceIds(@RequestBody IotReq req) {
//        if(StringUtils.isNullOrEmpty(req.getEnergyType())){
//            return Flux.fromIterable(iotService.getDeviceIds(req.getQuery()))
//                .flatMap(device ->{
//                    Term productTerm = new Term();
//                    productTerm.setTermType("eq");
//                    productTerm.setColumn("id");
//                    productTerm.setValue(device.getProductId());
//                    return Flux.fromIterable(iotService.getProduct(productTerm))
//                               .flatMap(product ->{
//                                   Term categoryTerm = new Term();
//                                   categoryTerm.setTermType("eq");
//                                   categoryTerm.setColumn("id");
//                                   categoryTerm.setValue(product.getClassifiedId());
//                                   return Flux.fromIterable(iotService.getProductCategory(categoryTerm))
//                                              .flatMap(category ->
//                                                           category.getKey().equals("Water") ||
//                                                               category.getKey().equals("Gas") ||
//                                                               category.getKey().equals("Electricity")
//                                                               ? Mono.empty() : Mono.just(device));
//                               });
//                });
//        } else {
//            return getEnergyTypeDevice(req);
//        }
//    }

    @Operation(summary = "获取设备ID以及关联能源表ID")
    @PostMapping("/getAllDeviceId")
    @Authorize(ignore = true)
    public Mono<Object> getAllDeviceId(@RequestBody DeviceInfoEntity entity){
        List<String> idLists = new ArrayList<>();
        return  service
            .createQuery()
            .where(DeviceInfoEntity::getDeviceId,entity.getDeviceId())
            .not(DeviceInfoEntity::getStatus,"1")
            .fetch()
            .doOnNext(v -> idLists.add(v.getDeviceId()))
            .flatMap(value -> service
                .createQuery()
                .where(DeviceInfoEntity::getParentId,value.getDeviceId())
                .not(DeviceInfoEntity::getStatus,"1")
                .fetch()
                .flatMap(child ->{
                    idLists.add(child.getDeviceId());
                    return  Flux.just(idLists);
                })
            ).distinct().collectList().flatMap(e-> Mono.just(e.stream().distinct()));
    }

    @Operation(summary = "校验设备Id是否唯一")
    @PostMapping("/check/only")
    @Authorize(ignore = true)
    public Mono<Object> CheckOnly(@RequestBody DeviceInfoEntity entity){
       return service
            .createQuery()
            .where(DeviceInfoEntity::getDeviceId,entity.getDeviceId())
            .and(DeviceInfoEntity::getStatus,"eq","0")
            .and(DeviceInfoEntity::getDeviceType,"0")
            .fetch()
            .collectList()
            .flatMap(v -> {
                if(v.size()==0){
                    return Mono.just("0");
                }
                return Mono.just("1");
            });
    }

//    private Flux<DeviceIotRes> getEnergyTypeDevice(IotReq req){
//        return Flux.fromIterable(iotService.getDeviceIds(req.getQuery()))
//                   .flatMap(device ->{
//                       Term productTerm = new Term();
//                       productTerm.setTermType("eq");
//                       productTerm.setColumn("id");
//                       productTerm.setValue(device.getProductId());
//                       return Flux.fromIterable(iotService.getProduct(productTerm))
//                                  .flatMap(product ->{
//                                      Term categoryTerm = new Term();
//                                      categoryTerm.setTermType("eq");
//                                      categoryTerm.setColumn("id");
//                                      categoryTerm.setValue(product.getClassifiedId());
//                                      return Flux.fromIterable(iotService.getProductCategory(categoryTerm))
//                                                 .flatMap(category ->
//                                                              category.getKey().equals(req.getEnergyType())
//                                                              ? Mono.just(device) : Mono.empty());
//                                  });
//                   });
//    }

    @Operation(summary = "校验用能设备是否被组合设备使用")
    @PostMapping("/queryIsDelete")
    @Authorize(ignore = true)
    public Mono<Object> queryIsDelete (@RequestBody DeviceInfoEntity entity){
        return unitDeviceService
            .createQuery()
            .where(CombinationUnitDeviceEntity::getDeviceId,entity.getDeviceId())
            .fetch()
            .collectList()
            .flatMap(v -> {
                if(v.size()==0){
                    return Mono.just("0");
                }
                return Mono.just("1");
            });
    }

//    @Operation(summary = "设备区域变动")
//    @PostMapping("area/update")
//    @ResourceAction(id= Permission.ACTION_UPDATE, name = "更新")
//    @Transactional(rollbackFor=Exception.class)
//    public Mono<Integer> deviceAreaUpdate(@RequestBody DeviceInfoEntity entity) {
//        return service
//            .createUpdate()
//            .set(DeviceInfoEntity::getStatus,"1")
//            .where(DeviceInfoEntity::getId,entity.getId())
//            .execute()
//            .flatMap(e -> {
//                if(e > 0){
//                    return service
//                        .createUpdate()
//                        .set(DeviceInfoEntity::getStatus,"1")
//                        .where(DeviceInfoEntity::getParentId,entity.getDeviceId())
//                        .execute()
//                        .then(testConfigDeviceService
//                                    .createDelete()
//                                    .where(TestConfigDeviceEntity::getDeviceId, entity.getDeviceId())
//                                    .execute());
//                }else {
//                    return Mono.empty();
//                }
//            })
//            .flatMap(a ->{
//                String id = String.valueOf(new SnowflakeIdWorker().nextId());
//                return Mono.just(entity)
//                           .doOnNext(value -> value.setId(id))
//                           .flatMap(value ->
//                                        Flux.fromIterable(value.getChild())
//                                            .doOnNext(child ->  {
//                                                child.setId(String.valueOf(new SnowflakeIdWorker().nextId()));
//                                                child.setMeterNumber(iotService.getMeterNumber(child.getDeviceId(),child.getEnergyType()));
//                                                child.setParentId(entity.getDeviceId());
//                                            })
//                                            .doOnError(throwable ->{throw new RuntimeException("设备表数不存在，请初始化表数");})
//                                            .collectList()
//                                            .flatMap(child -> service.insert(value)
//                                                                     .flatMap(insert->{
//                                                                         Collection<DeviceInfoEntity> collection = new ArrayList<>(child);
//                                                                         return service.insertBatch(Flux.just(collection));
//                                                                     })
//                                            )
//                           );
//            });
//
//    }

    @Operation(summary = "查询组合设备和设备")
    @PostMapping("_query/addUnitDevice")
    @QueryAction
    public Mono<PagerResult<DeviceInfoEntity>> queryAddUnitDevice(@RequestBody QueryParamEntity entity) {


        return service
            .query(entity.noPaging())
            .collectList()
            .flatMap(value -> {
                QueryParamEntity queryParamEntity = new QueryParamEntity();

                if(entity.getContext()!=null && entity.getContext().get("terms") != null){
                    String terms = JSONObject.toJSONString(entity.getContext().get("terms"));
                    List<Term>  terms2 = JSONObject.parseArray(terms, Term.class);
                    List<Term> filter = terms2.stream().filter(va -> !va.getColumn().equals("areaName") && !va.getColumn().equals("type")).collect(Collectors.toList());
                    queryParamEntity.setTerms(filter);
                }
                return queryHelper
                    .select("select cu.id id,cu.unit_name deviceName,cu.energy_type energyType," +"cu.device_status deviceStatus,"+
                                "cu.`status` `status`,GROUP_CONCAT(DISTINCT ar.area_name) areaName, " +
                                "cu.create_time as createTime from sems_combination_unit cu\n" +
                        "left join sems_combination_unit_device cud\n" +
                        "on cu.id = cud.unit_id\n" +
                        "left join sems_device_info de\n" +
                        "on de.device_id = cud.device_id\n" +
                        "left join area_info ar\n" +
                        "on ar.id = de.area_id\n" +
                        "where  de.status ='0' AND de.device_type = '0'\n" +
                        "group by cu.id", DeviceInfoEntity::new)
                    .where(queryParamEntity)
                    .fetch()
                    .filter(val->{
                        if(entity.getContext()!=null && entity.getContext().get("terms")!= null) {
                            String terms = JSONObject.toJSONString(entity.getContext().get("terms"));
                            List<Term> terms2 = JSONObject.parseArray(terms, Term.class);
                            List<Term> areaName = terms2.stream().filter(va -> va.getColumn().equals("areaName")).collect(Collectors.toList());
                            if(!areaName.isEmpty()){
                                Term filter = areaName.get(0);
                                List<String> strings = Arrays.asList(filter.getValue().toString().split(","));
                                return Arrays.asList(val.getAreaName().split(",")).containsAll(strings);
                            }
                        }
                        return true;

                    })
                    .collectList()
                    .flatMap(va -> {
                        Term term =null;
                        if(entity.getContext()!= null ){
                            String terms = JSONObject.toJSONString(entity.getContext().get("terms"));
                            List<Term> terms2 = JSONObject.parseArray(terms, Term.class);
                            List<Term> typeList = terms2.stream().filter(va1 -> va1.getColumn().equals("type")).collect(Collectors.toList());
                           if(!typeList.isEmpty()){
                               term = typeList.get(0);
                           }
                        }

                        va.addAll(value);
                        int pageNo = entity.getPageIndex();
                        int pageSize = entity.getPageSize();
                        int total = va.size();
                            //分页
                        List<DeviceInfoEntity> subList = va.stream().sorted(Comparator.comparing(DeviceInfoEntity::getCreateTime).reversed()).skip((pageNo - 1) * pageSize).limit(pageSize).
                            collect(Collectors.toList());
                        if(term!=null){
                            String type = term.getValue().toString();
                            if("1".equals(type)){
                                //数据
                                List<DeviceInfoEntity> data = va.stream().filter(v -> v.getAreaName() != null).collect(Collectors.toList());
                                //分页
                                List<DeviceInfoEntity> unTypeList =data.stream().sorted(Comparator.comparing(DeviceInfoEntity::getCreateTime).reversed()).skip((pageNo - 1) * pageSize).limit(pageSize).
                                    collect(Collectors.toList());
                                return Mono.just(PagerResult.of(data.size(),unTypeList,entity));
                            }else if("2".equals(type)){
                                //数据
                                List<DeviceInfoEntity> data = va.stream().filter(v -> "1".equals(v.getImportance())).collect(Collectors.toList());
                                //分页
                                List<DeviceInfoEntity> importentType = data.stream().sorted(Comparator.comparing(DeviceInfoEntity::getCreateTime).reversed()).skip((pageNo - 1) * pageSize).limit(pageSize).
                                    collect(Collectors.toList());
                                return Mono.just(PagerResult.of(data.size(),importentType,entity));
                            }else {
                                //数据
                                List<DeviceInfoEntity> data = va.stream().filter(v -> v.getAreaName() == null).collect(Collectors.toList());
                                //分页
                                List<DeviceInfoEntity> oneListType = data.stream().sorted(Comparator.comparing(DeviceInfoEntity::getCreateTime).reversed()).skip((pageNo - 1) * pageSize).limit(pageSize).
                                    collect(Collectors.toList());
                                return Mono.just(PagerResult.of(data.size(),oneListType,entity));

                            }
                        }else {
                            return Mono.just(PagerResult.of(total,subList,entity));
                        }

                    });
            });
    }


//    @Operation(summary = "新增表计设备")
//    @PostMapping("insert/meter/device")
//    @SaveAction
//    public Mono<Integer> insertMeterDevice(@RequestBody DeviceInfoEntity entity) {
//        return Mono.just(entity)
//                   .doOnNext(value -> {
//                       value.setMeterNumber(iotService.getMeterNumber(value.getDeviceId(),value.getEnergyType()));
//                       value.setDeviceType("1");
//                   })
//                   .flatMap(service::insert);
//    }

    @Operation(summary = "删除表记设备")
    @PostMapping("delete/meter/device")
    @DeleteAction
    public Mono<Integer> deleteMeterDevice(@RequestBody DeviceInfoEntity entity) {
        return service
            .createUpdate()
            .set(DeviceInfoEntity::getStatus,"1")
            .where(DeviceInfoEntity::getId,entity.getId())
            .execute();
    }

    @Operation(summary = "校验表记设备Id是否唯一")
    @PostMapping("/check/meter/only")
    @Authorize(ignore = true)
    public Mono<Object> CheckMeterOnly(@RequestBody DeviceInfoEntity entity){
        return service
            .createQuery()
            .where(DeviceInfoEntity::getDeviceId,entity.getDeviceId())
            .and(DeviceInfoEntity::getStatus,"eq","0")
            .and(DeviceInfoEntity::getDeviceType,"1")
            .fetch()
            .collectList()
            .flatMap(v -> {
                if(v.size()==0){
                    return Mono.just("0");
                }
                return Mono.just("1");
            });
    }


}
