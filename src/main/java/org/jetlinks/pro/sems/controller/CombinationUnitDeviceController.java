package org.jetlinks.pro.sems.controller;

import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.annotation.*;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.pro.sems.entity.req.ComBinationUnitDeviceReq;
import org.jetlinks.pro.sems.entity.req.UnitDeviceReq;
import org.jetlinks.pro.sems.entity.res.DevicePieRes;
import org.jetlinks.pro.sems.entity.res.UnitDeviceRes;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.jetlinks.pro.sems.entity.CombinationUnitDeviceEntity;
import org.jetlinks.pro.sems.entity.CombinationUnitEntity;
import org.jetlinks.pro.sems.entity.DeviceInfoEntity;
import org.jetlinks.pro.sems.service.*;
import org.jetlinks.pro.sems.utils.SnowflakeIdWorker;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/sems/unit/device")
@AllArgsConstructor
@Getter
@Tag(name = "组合设备映射1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "unit-device-mapping", name = "组合设备")
public class CombinationUnitDeviceController implements AssetsHolderCrudController<CombinationUnitDeviceEntity,String> {

    private final DeviceService deviceService;

    private final WaterConsumeService waterConsumeService;

    private final GasConsumeService gasConsumeService;

    private final ElectricityConsumeService electricityConsumeService;

    private final CombinationUnitService unitService;

    private final CombinationUnitDeviceService service;

    private final QueryHelper queryHelper;

    @Operation(summary = "新增组合设备且绑定映射")
    @PostMapping("/_insert/mapping")
    @SaveAction
    public Flux<Object> insertMapping(@RequestBody ComBinationUnitDeviceReq req){
        return Flux.just(new CombinationUnitEntity())
            .flatMap(data ->{
                String id = String.valueOf(new SnowflakeIdWorker().nextId());
                data.setId(id);
                req.setId(id);
                data.setUnitName(req.getUnitName());
                data.setEnergyType(req.getEnergyType());
                List<String> deviceIds = Arrays.asList(req.getDeviceId());
                List<CombinationUnitDeviceEntity> list = new ArrayList<>();
                for (String deviceId :deviceIds){
                    CombinationUnitDeviceEntity deviceEntity = new CombinationUnitDeviceEntity();
                    deviceEntity.setDeviceId(deviceId);
                    deviceEntity.setUnitId(req.getId());
                    list.add(deviceEntity);
                }
                Flux<Collection<CombinationUnitDeviceEntity>> content = Flux.just(list);
                return unitService
                    .insert(data)
                    .flatMap(e ->{
                        return service.insertBatch(content);
                    })
                    .then( queryHelper
                    .select("select count(*)  `count`  from sems_device_info\n" +
                                "where status='0' and device_status='0' and device_id in (select device_id from sems_combination_unit_device\n" +
                                "where unit_id in(select unit_id from sems_combination_unit_device\n" +
                                "where unit_id = '"+id+"'))" +
                                "and status='0' and device_status ='0'", UnitDeviceRes::new)
                    .fetch()
                    .flatMap(count -> {
                        if(String.valueOf(count.getCount()).equals("0")){
                            //如果设备状态全部为在线，修改组合设备的状态为在线
                            return unitService.createUpdate()
                                              .set(CombinationUnitEntity::getDeviceStatus,"1")
                                              .where(CombinationUnitDeviceEntity::getId,id)
                                              .execute();
                        }
                        //有一个是离线，组合设备即为离线
                        return unitService.createUpdate()
                                          .set(CombinationUnitEntity::getDeviceStatus,"0")
                                          .where(CombinationUnitDeviceEntity::getId,id)
                                          .execute();
                    }).then());
            });
    }


    @Operation(summary = "编辑组合设备且修改绑定映射")
    @PostMapping("/_update/mapping")
    @ResourceAction(id= Permission.ACTION_UPDATE, name = "更新")
    public Flux<Object> updateMapping(@RequestBody ComBinationUnitDeviceReq req){
        return Flux.just(new CombinationUnitEntity())
            .flatMap(data ->{
                data.setId(req.getId());
                req.setId(req.getId());
                data.setUnitName(req.getUnitName());
                data.setEnergyType(req.getEnergyType());
                List<String> deviceIds = Arrays.asList(req.getDeviceId());
                List<CombinationUnitDeviceEntity> list = new ArrayList<>();
                for (String deviceId :deviceIds){
                    CombinationUnitDeviceEntity deviceEntity = new CombinationUnitDeviceEntity();
                    deviceEntity.setDeviceId(deviceId);
                    deviceEntity.setUnitId(req.getId());
                    list.add(deviceEntity);
                }
                Flux<Collection<CombinationUnitDeviceEntity>> content = Flux.just(list);
                return unitService
                    .insert(data)
                    .flatMap(e ->{
                        return service.insertBatch(content);
                    })
                    .then( queryHelper
                               .select("select count(*)  `count`  from sems_device_info\n" +
                                           "where status='0' and device_status='0' and device_id in (select device_id from sems_combination_unit_device\n" +
                                           "where unit_id in(select unit_id from sems_combination_unit_device\n" +
                                           "where unit_id = '"+req.getId()+"'))" +
                                           "and status='0' and device_status ='0'", UnitDeviceRes::new)
                               .fetch()
                               .flatMap(count -> {
                                   if(String.valueOf(count.getCount()).equals("0")){
                                       //如果设备状态全部为在线，修改组合设备的状态为在线
                                       return unitService.createUpdate()
                                                         .set(CombinationUnitEntity::getDeviceStatus,"1")
                                                         .where(CombinationUnitDeviceEntity::getId,req.getId())
                                                         .execute();
                                   }
                                   //有一个是离线，组合设备即为离线
                                   return unitService.createUpdate()
                                                     .set(CombinationUnitEntity::getDeviceStatus,"0")
                                                     .where(CombinationUnitDeviceEntity::getId,req.getId())
                                                     .execute();
                               }).then());
            });
    }


    @Operation(summary = "删除组合设备且解除映射")
    @PostMapping("/_delete/mapping")
    @DeleteAction
    public Mono<Integer> deleteMapping(@RequestBody ComBinationUnitDeviceReq req){
        String unitId = req.getId();
        return unitService
                    .deleteById(unitId)
                    .then(service
                        .createDelete()
                        .where(CombinationUnitDeviceEntity::getUnitId,unitId)
                        .execute()
                    );
    }

    @Operation(summary = "获取组合设备关联的所有设备ID")
    @PostMapping("/getUnitDeviceId")
    @QueryAction
    public Flux<Object> getUnitDeviceId(@RequestBody ComBinationUnitDeviceReq req){
        return Flux.just(new ArrayList<>())
            .flatMap(data -> service
                .createQuery()
                .where(CombinationUnitDeviceEntity::getUnitId,req.getId())
                .fetch()
                .flatMapIterable(va -> {
                    data.add(va.getDeviceId());
                    return data;
                })
            ).distinct();
    }

    @Operation(summary = "获取组合设备能耗比例饼图")
    @Authorize(ignore = true)
    @PostMapping("/queryConsumePie")
    @QueryAction
    public Flux<Object> queryConsumePie(@RequestBody UnitDeviceReq req){
        String type = req.getType();
        return queryHelper
            .select("SELECT\n" +
                "\twc.`gather_time` AS `gatherTime`,\n" +
                "\twc.`difference` AS `difference`,\n" +
                "\twc.`device_id` AS `deviceId`,\n" +
                "\tany_value ( de.device_name ) `deviceName` \n" +
                "FROM\n" +
                "\tsems_combination_unit cu\n" +
                "\tLEFT JOIN sems_combination_unit_device cud ON cu.id = cud.unit_id\n" +
                "\tLEFT JOIN sems_"+type+"_consume wc ON cud.device_id = wc.device_id\n" +
                "\tLEFT JOIN sems_device_info de ON wc.device_id = de.device_id \n" +
                "WHERE\n" +
                "\twc.device_id IS NOT NULL", UnitDeviceRes::new)
            .where(req.getQuery())
            .fetch()
            .distinct()
            .collectList()
            .flatMapMany(data -> {
                //有数据才进行下面操作
                if(data.size()>0){
                    //存放返回的集合
                    List<DevicePieRes> devicePieRes = new ArrayList<>();
                    //计算总能耗
                    BigDecimal total =  new BigDecimal(data.stream()
                        .filter(v -> v.getDifference()!=null)
                        .mapToDouble(UnitDeviceRes::getDifference)
                        .sum());

                    //通过设备名称排序并将对应的能耗相加
                    Map<String, Double> collect = data
                        .stream()
                        .distinct()
                        .filter(va -> va.getDifference() != null)
                        .collect(Collectors.groupingBy(e -> e.getDeviceName(), Collectors.summingDouble(UnitDeviceRes::getDifference)));
                    //遍历Map进行数据添加
                    return this.emptyDetail(req)
                        .collectList()
                        .flatMapMany(va->{
                            for (DevicePieRes pieRes : va) {
                                if(collect.get(pieRes.getDeviceName())!=null){
                                    DevicePieRes device = new DevicePieRes();
                                    BigDecimal difference = new BigDecimal(collect.get(pieRes.getDeviceName())).divide(BigDecimal.valueOf(1),2,BigDecimal.ROUND_HALF_UP);
                                    BigDecimal rate =BigDecimal.ZERO;
                                    if(total.compareTo(BigDecimal.ZERO)>0){
                                        rate = difference.divide(total, 4, BigDecimal.ROUND_HALF_DOWN);
                                    }
                                    device.setDeviceName(pieRes.getDeviceName());
                                    device.setRate(rate);
                                    device.setDifference(difference);
                                    devicePieRes.add(device);
                                }else {
                                    DevicePieRes device = new DevicePieRes();
                                    device.setDeviceName(pieRes.getDeviceName());
                                    device.setRate(BigDecimal.ZERO);
                                    device.setDifference(BigDecimal.ZERO);
                                    devicePieRes.add(device);
                                }
                            }
                            List<DevicePieRes> collect1 = devicePieRes.stream().sorted(Comparator.comparing(DevicePieRes::getDifference).reversed()).collect(Collectors.toList());

                            return Flux.fromIterable(collect1);
                        });

//                    for(String deviceName:collect.keySet()){
//                        DevicePieRes device = new DevicePieRes();
//                        BigDecimal difference = new BigDecimal(collect.get(deviceName)).divide(BigDecimal.valueOf(1),2,BigDecimal.ROUND_HALF_UP);
//                        BigDecimal rate =BigDecimal.ZERO;
//                        if(total.compareTo(BigDecimal.ZERO)>0){
//                             rate = difference.divide(total, 4, BigDecimal.ROUND_HALF_DOWN);
//                        }
//                        device.setDeviceName(deviceName);
//                        device.setRate(rate);
//                        device.setDifference(difference);
//                        devicePieRes.add(device);
//                    }
//
                }
                return emptyDetail(req);
            });
    }

    public Flux<DevicePieRes> emptyDetail(UnitDeviceReq req){
        QueryParamEntity query = req.getQuery();
        List<Term> collect = query.getTerms().stream().filter(v -> !"gatherTime".equals(v.getColumn())).collect(Collectors.toList());
        query.setTerms(collect);
        ArrayList<DevicePieRes> devicePieRes = new ArrayList<>();
        return queryHelper
            .select("SELECT\n" +
                "\twc.`gather_time` AS `gatherTime`,\n" +
                "\twc.`difference` AS `difference`,\n" +
                "\twc.`device_id` AS `deviceId`,\n" +
                "\tany_value ( de.device_name ) `deviceName` \n" +
                "FROM\n" +
                "\tsems_combination_unit cu\n" +
                "\tLEFT JOIN sems_combination_unit_device cud ON cu.id = cud.unit_id\n" +
                "\tLEFT JOIN sems_"+req.getType()+"_consume wc ON cud.device_id = wc.device_id\n" +
                "\tLEFT JOIN sems_device_info de ON wc.device_id = de.device_id \n" +
                "WHERE\n" +
                "\twc.device_id IS NOT NULL", UnitDeviceRes::new)
            .where(req.getQuery())
            .fetch()
            .map(UnitDeviceRes::getDeviceName)
            .distinct()
            .flatMap(list->{
                DevicePieRes devicePieResEmpty = new DevicePieRes();
                devicePieResEmpty.setDeviceName(list);
                devicePieResEmpty.setDifference(BigDecimal.ZERO);
                devicePieResEmpty.setRate(BigDecimal.ZERO);
                return Mono.just(devicePieResEmpty);
            });
    }

    @Operation(summary = "获取组合设备能耗趋势")
    @Authorize(ignore = true)
    @PostMapping("/queryConsumeTrend")
    @QueryAction
    public Flux<Object> queryConsumeTrend (@RequestBody UnitDeviceReq req){
        String type = req.getType();
        if(req.getStyle().equals("1")){
            return queryHelper
                .select("SELECT\n" +
                    "DATE(FROM_UNIXTIME(ROUND(wc.gather_time/1000,0))) as gatherTime,\n" +
                    "any_value (sum(wc.`difference`)) AS `difference`,\n" +
                    "any_value (wc.`device_id` )AS `deviceId`,\n" +
                    "any_value(cu.`id`) AS `id`,\n" +
                    "any_value ( de.device_name ) `deviceName` \n" +
                    "FROM\n" +
                    "sems_combination_unit cu\n" +
                    "LEFT JOIN sems_combination_unit_device cud ON cu.id = cud.unit_id\n" +
                    "LEFT JOIN sems_"+type+"_consume wc ON cud.device_id = wc.device_id\n" +
                    "LEFT JOIN sems_device_info de ON wc.device_id = de.device_id \n" +
                    "WHERE\n" +
                    "wc.device_id IS NOT NULL and de.status != 1\n" +
                    "GROUP BY DATE(FROM_UNIXTIME(ROUND(wc.gather_time/1000,0))),de.device_name", UnitDeviceRes::new)
                .where(req.getQuery())
                .fetch()
                .distinct()
                .collectList()
                .flatMapMany(data -> {
                    //有数据才进行下面操作
                    if(data.size()>0){
                        //获取所有设备名称集合
                        Set<String> deviceNameSet = data.stream().map(UnitDeviceRes::getDeviceName).collect(Collectors.toSet());
                        //通过时间戳分组
                        LinkedHashMap<Long, List<UnitDeviceRes>> collect = data
                            .stream()
                            .distinct()
                            .filter(va -> va.getDifference() != null)
                            .sorted(Comparator.comparing(UnitDeviceRes::getGatherTime))
                            .collect(Collectors.groupingBy(e -> e.getGatherTime(), LinkedHashMap::new, Collectors.toList()));

                        //时间处理
                        //所有时间集合
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                        //获取两个时间之间的所有时间集合
                        List<Long> timeList = new ArrayList<>();
                        Calendar calendar = Calendar.getInstance();
                        Date startDate =null;
                        Date endDate =null;
                        //获取前端传的时间范围
                        String dateTime = req.getQuery()
                            .getTerms()
                            .stream()
                            .filter(term -> "btw".equals(term.getTermType()))
                            .findFirst()
                            .map(Term::getValue)
                            .map(Object::toString)
                            .orElseGet(() -> {
                                LocalDate currentDate = LocalDate.now();
                                LocalDateTime startOfDay = LocalDateTime.of(currentDate, LocalTime.MIN);
                                LocalDateTime endOfDay = LocalDateTime.of(currentDate, LocalTime.MAX);
                                long startOfDayTimestamp = startOfDay.toEpochSecond(ZoneOffset.UTC);
                                long endOfDayTimestamp = endOfDay.toEpochSecond(ZoneOffset.UTC);
                                return startOfDayTimestamp + "," + endOfDayTimestamp;
                            });
                        List<Long> longs = JSON.parseArray(dateTime, Long.class);


                        try {
                            startDate = format.parse(format.format(longs.get(0)));
                            endDate = format.parse(format.format(longs.get(1)));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        while (startDate.getTime() <= endDate.getTime()) {
                            //把日期添加进集合
                            timeList.add(startDate.getTime());
                            //设置日期
                            calendar.setTime(startDate);
                            //把日期增加一条
                            calendar.add(Calendar.DATE, 1);
                            //获取增加后的日期
                            startDate = calendar.getTime();
                        }

                        for (Long s : timeList) {
                            if(collect.get(s) == null){
                                collect.put(s,new ArrayList<>());
                            }
                        }
                        TreeMap<Long, List<UnitDeviceRes>> treeMap = new TreeMap<>(collect);

                        //先对名称进行遍历
                        return this.tredEmptyDetailName(req)
                            .flatMapMany(value->{
                                for(String deviceName:value){
                                    //再对数据集合进行遍历
                                    for(Object gatherTime:treeMap.keySet()){
                                        //判断是否具有该名称的数据
                                        boolean result = treeMap.get(gatherTime).stream().anyMatch(m -> m.getDeviceName().equals(deviceName));
                                        //如果没有就进行添加
                                        if(!result){
                                            UnitDeviceRes unitDeviceRes = new UnitDeviceRes();
                                            unitDeviceRes.setDeviceName(deviceName);
                                            unitDeviceRes.setDifference(0.0);
                                            unitDeviceRes.setGatherTime(Long.getLong(String.valueOf(gatherTime)));
                                            treeMap.get(gatherTime).add(unitDeviceRes);
                                        }
                                    }
                                }
                                return Flux.just(treeMap);
                            });
                    }
                    return this.tredEmptyDetail(req);
                });
        }
        return queryHelper
            .select("SELECT\n" +
                "\twc.`gather_time` AS `gatherTime`,\n" +
                "\twc.`difference` AS `difference`,\n" +
                "\twc.`device_id` AS `deviceId`,\n" +
                "\tany_value ( de.device_name ) `deviceName` \n" +
                "FROM\n" +
                "\tsems_combination_unit cu\n" +
                "\tLEFT JOIN sems_combination_unit_device cud ON cu.id = cud.unit_id\n" +
                "\tLEFT JOIN sems_"+type+"_consume wc ON cud.device_id = wc.device_id\n" +
                "\tLEFT JOIN sems_device_info de ON wc.device_id = de.device_id \n" +
                "WHERE\n" +
                "\twc.device_id IS NOT NULL", UnitDeviceRes::new)
            .where(req.getQuery())
            .fetch()
            .distinct()
            .collectList()
            .flatMapMany(data -> {
                //有数据才进行下面操作
                if(data.size()>0){
                    //获取所有设备名称集合
                    Set<String> deviceNameSet = data.stream().map(UnitDeviceRes::getDeviceName).collect(Collectors.toSet());
                    //通过时间戳分组
                    LinkedHashMap<Long, List<UnitDeviceRes>> collect = data
                        .stream()
                        .distinct()
                        .filter(va -> va.getDifference() != null)
                        .sorted(Comparator.comparing(UnitDeviceRes::getGatherTime))
                        .collect(Collectors.groupingBy(e -> e.getGatherTime(), LinkedHashMap::new, Collectors.toList()));


                    //先对名称进行遍历
                    return this.tredEmptyDetailName(req)
                        .flatMapMany(valu->{
                            for(String deviceName:valu){
                                //再对数据集合进行遍历
                                for(Object gatherTime:collect.keySet()){
                                    //判断是否具有该名称的数据
                                    boolean result = collect.get(gatherTime).stream().anyMatch(m -> m.getDeviceName().equals(deviceName));
                                    //如果没有就进行添加
                                    if(!result){
                                        UnitDeviceRes unitDeviceRes = new UnitDeviceRes();
                                        unitDeviceRes.setDeviceName(deviceName);
                                        unitDeviceRes.setDifference(0.0);
                                        unitDeviceRes.setGatherTime(Long.getLong(String.valueOf(gatherTime)));
                                        collect.get(gatherTime).add(unitDeviceRes);
                                    }
                                }
                            }
                            return Flux.just(collect);
                        });

                }
                return this.tredEmptyDetail(req);
            });
    }


    /**
     * 获取组合设备能耗趋势数据为空处理
     * @param req
     * @return
     */
    public Flux<Object> tredEmptyDetail(UnitDeviceReq req){
        QueryParamEntity query = req.getQuery();
        List<Term> collect = query.getTerms().stream().filter(v -> !"gatherTime".equals(v.getColumn())).collect(Collectors.toList());
        query.setTerms(collect);
        String type = req.getType();
        if(req.getStyle().equals("1")) {
            return queryHelper
                .select("SELECT\n" +
                    "DATE(FROM_UNIXTIME(ROUND(wc.gather_time/1000,0))) as gatherTime,\n" +
                    "any_value (sum(wc.`difference`)) AS `difference`,\n" +
                    "any_value (wc.`device_id` )AS `deviceId`,\n" +
                    "any_value(cu.`id`) AS `id`,\n" +
                    "any_value ( de.device_name ) `deviceName` \n" +
                    "FROM\n" +
                    "sems_combination_unit cu\n" +
                    "LEFT JOIN sems_combination_unit_device cud ON cu.id = cud.unit_id\n" +
                    "LEFT JOIN sems_" + type + "_consume wc ON cud.device_id = wc.device_id\n" +
                    "LEFT JOIN sems_device_info de ON wc.device_id = de.device_id \n" +
                    "WHERE\n" +
                    "wc.device_id IS NOT NULL and de.status != 1\n" +
                    "GROUP BY DATE(FROM_UNIXTIME(ROUND(wc.gather_time/1000,0))),de.device_name", UnitDeviceRes::new)
                .where(req.getQuery())
                .fetch()
                .distinct()
                .collectList()
                .flatMapMany(data->{

                    //获取所有设备名称集合
                    Set<String> deviceNameSet = data.stream().map(UnitDeviceRes::getDeviceName).collect(Collectors.toSet());
                    //通过时间戳分组
                    LinkedHashMap<Long, List<UnitDeviceRes>> timeList = data
                        .stream()
                        .distinct()
                        .filter(va -> va.getDifference() != null)
                        .sorted(Comparator.comparing(UnitDeviceRes::getGatherTime))
                        .collect(Collectors.groupingBy(e -> e.getGatherTime(), LinkedHashMap::new, Collectors.toList()));

                    TreeMap<Long, List<UnitDeviceRes>> treeMap = new TreeMap<>(timeList);

                    //先对名称进行遍历
                    for(String deviceName:deviceNameSet){
                        //再对数据集合进行遍历
                        for(Object gatherTime:treeMap.keySet()){
                            //判断是否具有该名称的数据
                            boolean result = treeMap.get(gatherTime).stream().anyMatch(m -> m.getDeviceName().equals(deviceName));
                            //如果没有就进行添加
                            if(!result){
                                UnitDeviceRes unitDeviceRes = new UnitDeviceRes();
                                unitDeviceRes.setDeviceName(deviceName);
                                unitDeviceRes.setDifference(0.0);
                                unitDeviceRes.setGatherTime(Long.getLong(String.valueOf(gatherTime)));
                                treeMap.get(gatherTime).add(unitDeviceRes);
                            }
                        }
                    }
                    return Flux.just(treeMap);
                });
        }else {
            return queryHelper
                .select("SELECT\n" +
                    "\twc.`gather_time` AS `gatherTime`,\n" +
                    "\twc.`difference` AS `difference`,\n" +
                    "\twc.`device_id` AS `deviceId`,\n" +
                    "\tany_value ( de.device_name ) `deviceName` \n" +
                    "FROM\n" +
                    "\tsems_combination_unit cu\n" +
                    "\tLEFT JOIN sems_combination_unit_device cud ON cu.id = cud.unit_id\n" +
                    "\tLEFT JOIN sems_"+type+"_consume wc ON cud.device_id = wc.device_id\n" +
                    "\tLEFT JOIN sems_device_info de ON wc.device_id = de.device_id \n" +
                    "WHERE\n" +
                    "\twc.device_id IS NOT NULL", UnitDeviceRes::new)
                .where(req.getQuery())
                .fetch()
                .distinct()
                .collectList()
                .flatMapMany(data->{
                    //获取所有设备名称集合
                    Set<String> deviceNameSet = data.stream().map(UnitDeviceRes::getDeviceName).collect(Collectors.toSet());
                    //通过时间戳分组
                    LinkedHashMap<Long, List<UnitDeviceRes>> timeList = data
                        .stream()
                        .distinct()
                        .filter(va -> va.getDifference() != null)
                        .sorted(Comparator.comparing(UnitDeviceRes::getGatherTime))
                        .collect(Collectors.groupingBy(e -> e.getGatherTime(), LinkedHashMap::new, Collectors.toList()));


                    //先对名称进行遍历
                    for(String deviceName:deviceNameSet){
                        //再对数据集合进行遍历
                        for(Object gatherTime:timeList.keySet()){
                            //判断是否具有该名称的数据
                            boolean result = timeList.get(gatherTime).stream().anyMatch(m -> m.getDeviceName().equals(deviceName));
                            //如果没有就进行添加
                            if(!result){
                                UnitDeviceRes unitDeviceRes = new UnitDeviceRes();
                                unitDeviceRes.setDeviceName(deviceName);
                                unitDeviceRes.setDifference(0.0);
                                unitDeviceRes.setGatherTime(Long.getLong(String.valueOf(gatherTime)));
                                timeList.get(gatherTime).add(unitDeviceRes);
                            }
                        }
                    }
                    return Flux.just(timeList);
                });
        }
    }

    public Mono<Set<String>> tredEmptyDetailName(UnitDeviceReq req){
        QueryParamEntity query = req.getQuery();
        List<Term> collect = query.getTerms().stream().filter(v -> !"gatherTime".equals(v.getColumn())).collect(Collectors.toList());
        query.setTerms(collect);
        String type = req.getType();
        if(req.getStyle().equals("1")) {
            return queryHelper
                .select("SELECT\n" +
                    "DATE(FROM_UNIXTIME(ROUND(wc.gather_time/1000,0))) as gatherTime,\n" +
                    "any_value (sum(wc.`difference`)) AS `difference`,\n" +
                    "any_value (wc.`device_id` )AS `deviceId`,\n" +
                    "any_value(cu.`id`) AS `id`,\n" +
                    "any_value ( de.device_name ) `deviceName` \n" +
                    "FROM\n" +
                    "sems_combination_unit cu\n" +
                    "LEFT JOIN sems_combination_unit_device cud ON cu.id = cud.unit_id\n" +
                    "LEFT JOIN sems_" + type + "_consume wc ON cud.device_id = wc.device_id\n" +
                    "LEFT JOIN sems_device_info de ON wc.device_id = de.device_id \n" +
                    "WHERE\n" +
                    "wc.device_id IS NOT NULL and de.status != 1\n" +
                    "GROUP BY DATE(FROM_UNIXTIME(ROUND(wc.gather_time/1000,0))),de.device_name", UnitDeviceRes::new)
                .where(req.getQuery())
                .fetch()
                .collectList()
                .flatMap(data->{

                    //获取所有设备名称集合
                    Set<String> deviceNameSet = data.stream().map(UnitDeviceRes::getDeviceName).collect(Collectors.toSet());
                 return Mono.just(deviceNameSet);
                });
        }else {
            return queryHelper
                .select("SELECT\n" +
                    "\twc.`gather_time` AS `gatherTime`,\n" +
                    "\twc.`difference` AS `difference`,\n" +
                    "\twc.`device_id` AS `deviceId`,\n" +
                    "\tany_value ( de.device_name ) `deviceName` \n" +
                    "FROM\n" +
                    "\tsems_combination_unit cu\n" +
                    "\tLEFT JOIN sems_combination_unit_device cud ON cu.id = cud.unit_id\n" +
                    "\tLEFT JOIN sems_"+type+"_consume wc ON cud.device_id = wc.device_id\n" +
                    "\tLEFT JOIN sems_device_info de ON wc.device_id = de.device_id \n" +
                    "WHERE\n" +
                    "\twc.device_id IS NOT NULL", UnitDeviceRes::new)
                .where(req.getQuery())
                .fetch()
                .collectList()
                .flatMap(data->{
                    //获取所有设备名称集合
                    Set<String> deviceNameSet = data.stream().map(UnitDeviceRes::getDeviceName).collect(Collectors.toSet());
                    return Mono.just(deviceNameSet);
                });
        }
    }


    @Operation(summary = "根据组合设备id获取绑定的设备及能源类型")
    @GetMapping("/getUnitDevice/{unitId}")
    @QueryAction
    public Flux<CombinationUnitDeviceEntity> getCombinationUnitDeviceEnergyType(@PathVariable("unitId") String unitId){
                return service
                    .createQuery()
                    .where(CombinationUnitDeviceEntity::getUnitId,unitId)
                    .fetch()
                    .flatMap(i->{
                        return deviceService
                            .createQuery()
                            .where(DeviceInfoEntity::getDeviceId,i.getDeviceId())
                            .fetch()
                            .doOnNext(value->i.setEnergyType(value.getEnergyType()))
                            .then(Mono.just(i));
                    });

    }


}
