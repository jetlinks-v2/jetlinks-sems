package org.jetlinks.project.busi.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.DeviceInfoEntity;
import org.jetlinks.project.busi.entity.DeviceStateEntity;
import org.jetlinks.project.busi.entity.ElectricityConsumeEntity;
import org.jetlinks.project.busi.entity.dto.HomePageDto;
import org.jetlinks.project.busi.entity.req.HomePageReq;
import org.jetlinks.project.busi.entity.req.HomePageTrendDetailReq;
import org.jetlinks.project.busi.entity.res.*;
import org.jetlinks.project.busi.service.*;
import org.jetlinks.project.busi.utils.DateUtil;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/***
 * 综合态势接口
 * @author Ljh
 */
@RestController
@RequestMapping("/sems/home/page")
@AllArgsConstructor
@Getter
@Tag(name = "综合态势1.0") //swagger
@Authorize(ignore = true)
@AssetsController(type = ExampleAssetType.TYPE_ID)
public class HomePageController{

    private final QueryHelper queryHelper;

    private final WaterConsumeService waterService;

    private final ElectricityConsumeService electService;

    private final GasConsumeService gasService;

    private final AreaInfoService areaInfoService;

    private final DeviceService deviceService;

    @Operation(summary = "查询累计能耗")
    @Authorize(ignore = true)
    @GetMapping("/_query/energy/consume")
    public Flux<AccumulateRes> queryEnergyConsume(){
       return Flux
            .just(new AccumulateRes())
           .flatMap(data -> waterService.getAccumulate().flatMap(va -> {
               data.setWater(va);
               return Flux.just(data);
            }))
            .flatMap(data -> gasService.getAccumulate().flatMap(va -> {
                data.setGas(va);
                return Flux.just(data);
            }))
           .flatMap(data -> electService.getAccumulate().flatMap(va -> {
               data.setElectricity(va);
               return Flux.just(data);
           }));
    }


    @Operation(summary = "查询实时能耗")
    @Authorize(ignore = true)
    @GetMapping(name="/_query/real/time/consume")
    public Flux<Object> realTimeEnergyConsume() {
        Long endTime = System.currentTimeMillis();
        Long startTime =endTime-60*60*1000*1;
        //Long startTime = DateUtil.getDailyStartTime(endTime,"GMT+8:00");
        //1. 构造SQL
        String sql="SELECT\n" +
                "    SUM(e.difference) AS electricityEnergyConsume,\n" +
                "    SUM(w.difference) AS waterEnergyConsume,\n" +
                "    SUM(g.difference) AS gasEnergyConsume\n" +
                "FROM\n" +
                "    sems_energy_gather eg\n" +
                "    LEFT JOIN sems_electricity_consume e ON eg.id = e.gather_id\n" +
                "    LEFT JOIN sems_water_consume w ON eg.id = w.gather_id\n" +
                "    LEFT JOIN sems_gas_consume g ON eg.id = g.gather_id;";
        //2.查询今天的能耗
        return areaInfoService.getAllAreaDeviceIds()
                              .distinct()
                              .collectList()
                              .flatMapMany(deviceIds ->
                                  queryHelper.select(sql,HomePageRealTimeRes::new)
                                             .where(dsl->dsl.gte("eg.gather_time",startTime)
                                                            .lte("eg.gather_time",endTime)
                                                            .nest(ds -> ds.or("w.device_id","in",deviceIds)
                                                                          .or("g.device_id","in",deviceIds)
                                                                          .or("e.device_id","in",deviceIds)))
                                             .fetch()
                                             .flatMap(now ->{
                                                 //2.1 今天的能耗空值处理
                                                 if(now.getWaterEnergyConsume()==null) now.setWaterEnergyConsume(BigDecimal.ZERO);
                                                 if(now.getElectricityEnergyConsume()==null) now.setElectricityEnergyConsume(BigDecimal.ZERO);
                                                 if(now.getGasEnergyConsume()==null) now.setGasEnergyConsume(BigDecimal.ZERO);
                                                 //3 查询昨天的能耗
                                                 return queryHelper.select(sql,HomePageRealTimeRes::new)
                                                                   .where(dsl->dsl.gte("eg.gather_time",startTime-86400000L)
                                                                                  .lte("eg.gather_time",endTime-86400000L)
                                                                                  .nest(ds -> ds.or("w.device_id","in",deviceIds)
                                                                                                .or("g.device_id","in",deviceIds)
                                                                                                .or("e.device_id","in",deviceIds)))
                                                                   .fetch()
                                                                   .flatMap(last -> {
                                                                       //4 昨天的能耗空值处理
                                                                       if(last.getWaterEnergyConsume()==null) last.setWaterEnergyConsume(BigDecimal.ZERO);
                                                                       if(last.getElectricityEnergyConsume()==null) last.setElectricityEnergyConsume(BigDecimal.ZERO);
                                                                       if(last.getGasEnergyConsume()==null) last.setGasEnergyConsume(BigDecimal.ZERO);
                                                                       //4.计算环比并返回
                                                                       return Flux.just(calculatePeriodOnPeriod(last,now));
                                                                   });
                                             })
                              );
    }

    @Operation(summary = "能耗排名")
    @PostMapping("/rank/{type}")
    public Flux<HomePageRankRes> rankQuery(@PathVariable String type) {
        String dimension;
        long startTime;
        Long endTime = System.currentTimeMillis();
        Instant instant = Instant.ofEpochMilli(endTime);
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("Asia/Shanghai"));
        LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();
        //1 获取所在的月份
        int month = localDateTime.getMonth().getValue();
        //2 判断是否小于5个月，若小于5个月则按日统计，若大于5个月则按月统计
        if(month<5){
            dimension = "%Y-%m-%d";
            startTime = DateUtil.getMonthStartTime(endTime,"GMT+8:00");
        }else {
            dimension = "%Y-%m";
            startTime = DateUtil.getYearStartTime(endTime,"GMT+8:00");
        }
        return areaInfoService.getAllAreaDeviceIds(type)
                              .collectList()
                              .flatMapMany(deviceIds ->
                                               //3 获取前5条数据
                                               queryHelper.select("SELECT\n" +
                                                                      "   FROM_UNIXTIME((gather_time) / 1000, '"+dimension+"') AS date,\n" +
                                                                      "    SUM(w.difference) AS totalEnergyConsumption\n" +
                                                                      "FROM\n" +
                                                                      "    sems_"+type+"_consume w\n" +
                                                                      "GROUP BY\n" +
                                                                      "    FROM_UNIXTIME((gather_time) / 1000, '"+dimension+"')\n" +
                                                                      "ORDER BY \n" +
                                                                      "totalEnergyConsumption DESC;", HomePageRankRes::new)
                                                          .where(dsl->dsl.lte("gather_time",endTime)
                                                                         .gte("gather_time",startTime)
                                                                        .in("w.device_id",deviceIds))
                                                          .fetch()
                                                          .collectList()
                                                          .flatMapMany(i -> {
                                                              //3.1 如果数据大于4个则直接展示
                                                              if(i.size()>4 ) return Flux.fromIterable(i.subList(0,5));
                                                              //3.2 数据不足4个，但是如果是以天为维度则直接展示
                                                              if(dimension.equals("%Y-%m-%d")) return Flux.fromIterable(i);
                                                              //3.3 以月为维度的，说明月不足四个，则以天为维度查询，查询这个月top5
                                                              return queryHelper.select("SELECT\n" +
                                                                                            "   FROM_UNIXTIME((gather_time) / 1000, '%Y-%m-%d') AS date,\n" +
                                                                                            "    SUM(w.difference) AS totalEnergyConsumption\n" +
                                                                                            "FROM\n" +
                                                                                            "    sems_"+type+"_consume w\n" +
                                                                                            "GROUP BY\n" +
                                                                                            "    FROM_UNIXTIME((gather_time) / 1000, '%Y-%m-%d')\n" +
                                                                                            "ORDER BY \n" +
                                                                                            "totalEnergyConsumption DESC;", HomePageRankRes::new)
                                                                                .where(dsl->{
//                                                                                    LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
                                                                                    dsl.lte("gather_time",endTime);
                                                                                    //获取这个月开始时间戳
                                                                                    dsl.gte("gather_time",DateUtil.getMonthStartTime(endTime,"GMT+8:00"));
                                                                                    dsl.in("w.device_id",deviceIds);
                                                                                })
                                                                                .fetch()
                                                                                .take(5);
                                                          }).doOnNext(res -> res.setType(type))
                              );
    }


    @Operation(summary = "能耗趋势分析")
    @GetMapping("/trend")
    public Flux<HomePageTrendRes> trendAnalysis(HomePageReq homePageReq) {
        String type=homePageReq.getType();
        Integer dateType = homePageReq.getDateType();
        HomePageDto homePageDto = new HomePageDto();
        String sql;
        HashMap<String,HomePageTrendInfo> timeMap = new HashMap<>();
        //1 获取时间维度、开始时间、结束时间以及存放数据的map
        getTimeDimension(homePageDto,dateType,timeMap);
        Long startTime = homePageDto.getStartTime();
        Long endTime = homePageDto.getEndTime();
        String dimension = homePageDto.getDimension();
        //2 构造SQL
        sql= "SELECT \n" +
            "    FROM_UNIXTIME(((gather_time) / 1000), '"+dimension+"') AS date,\n" +
            "    SUM(difference) AS number\n" +
            "FROM\n" +
            "    sems_"+ type +"_consume  where device_id='0'\n" +
            "GROUP BY \n" +
            "    FROM_UNIXTIME(((gather_time) / 1000), '"+dimension+"')\n" +
            "ORDER BY \n" +
            "    date;";
        return Flux.just(timeMap)
                   .flatMap(map ->//4 查询 统计
                                queryHelper
                                    .select(sql,HomePageTrendInfo::new)
                                    .where(dsl->dsl.gte("gather_time",startTime)
                                                   .lte("gather_time",endTime))
                                    .fetch()
                                    .collectList()
                                    .flatMapMany(homePageTrendInfos -> {
                                        BigDecimal average = BigDecimal.ZERO;
                                        BigDecimal peakNumber = BigDecimal.ZERO;
                                        if(homePageTrendInfos.size()>0){
                                            peakNumber = homePageTrendInfos.get(0).getNumber();
                                            //5 将查询到的数据更新map
                                            for (HomePageTrendInfo homePageTrendInfo : homePageTrendInfos) {
                                                //6 取最峰值
                                                if(homePageTrendInfo.getNumber().compareTo(peakNumber)==1){
                                                    peakNumber = homePageTrendInfo.getNumber();
                                                }
                                                if(dateType==1){
                                                    SimpleDateFormat dateFormat= new SimpleDateFormat("yyyy-MM-dd HH:00");
                                                    Date parse =null;
                                                    try {
                                                        parse = dateFormat.parse(homePageTrendInfo.getDate());
                                                    } catch (ParseException e) {
                                                        e.printStackTrace();
                                                    }
                                                    assert parse != null;
                                                    long timeTemp=parse.getTime()+1000*60*60;
                                                    Date date = new Date(timeTemp);
                                                    String format = dateFormat.format(date);
                                                    HomePageTrendInfo newHomePageTrend = new HomePageTrendInfo();
                                                    newHomePageTrend.setDate(format);
                                                    newHomePageTrend.setNumber(homePageTrendInfo.getNumber());
                                                    if(map.get(format)!=null){
                                                        map.replace(format,newHomePageTrend);
                                                    }
                                                    if(timeTemp>new Date().getTime()){
                                                        SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                                                        String format1 = dateFormat1.format(new Date());
                                                        HomePageTrendInfo newHomePageTrendE = new HomePageTrendInfo();
                                                        newHomePageTrendE.setDate(format1);
                                                        newHomePageTrendE.setNumber(homePageTrendInfo.getNumber());
                                                        map.replace(format1,newHomePageTrendE);
                                                    }
                                                }else {
                                                    if(map.get(homePageTrendInfo.getDate())!=null){
                                                        map.replace(homePageTrendInfo.getDate(),homePageTrendInfo);
                                                    }
                                                }
                                                average = average.add(homePageTrendInfo.getNumber());
                                            }
                                            //7 求取平均值
                                            average = average.divide(BigDecimal.valueOf(map.size()), BigDecimal.ROUND_DOWN);
                                        }
                                        //8 排序收集
                                        List<HomePageTrendInfo> collect = map
                                            .values()
                                            .stream()
                                            .sorted(Comparator.comparing(HomePageTrendInfo::getDate))
                                            .collect(Collectors.toList());
                                        return Flux.just(new HomePageTrendRes(collect, peakNumber, average,type));
                                    })
        );

    }

    @Operation(summary = "能耗趋势详情")
    @PostMapping("/trend/detail")
    public Flux<Object> trendDetail(@RequestBody HomePageTrendDetailReq homePageTrendDetailReq) {
        String type=homePageTrendDetailReq.getType();
        Long startTime= homePageTrendDetailReq.getStartTime();
        Long endTime= homePageTrendDetailReq.getEndTime();
        HashMap<String,HomePageTrendDetailRes> timeMap = new HashMap<>();
        //1 获取时间维度以及存放数据的map
        String dimension = getDetailDimension(startTime, endTime,timeMap);
        //2 构造SQL
        String sql= "SELECT \n" +
            "    FROM_UNIXTIME(((gather_time) / 1000), '"+dimension+"') AS date,\n" +
            "    SUM(difference) AS number\n" +
            "FROM\n" +
            "    sems_"+ type +"_consume\n" +
            "GROUP BY \n" +
            "    FROM_UNIXTIME(((gather_time) / 1000), '"+dimension+"')\n" +
            "ORDER BY \n" +
            "    date;";
        //3 获取所有区域该能源类型的所有设备
        return areaInfoService.getAllAreaDeviceIds(type)
                              .collectList()
                              .flatMapMany(deviceIds ->
                                               Flux.just(timeMap).flatMap(map ->
                                                                              //4 查询今年的数据
                                                                              queryHelper.select(sql,HomePageTrendInfo::new)
                                                                                         .where(dsl->dsl.gte("gather_time",startTime)
                                                                                                        .lte("gather_time",endTime)
                                                                                                        .in("device_id",deviceIds))
                                                                                         .fetch()
                                                                                         .collectList()
                                                                                         .flatMapMany(now ->{
                                                                                             SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH");
                                                                                             long day=((endTime - startTime)) / (24 * 3600 * 1000);
                                                                                             System.out.println("天数："+day);

                                                                                                 //5 将今年的数据更新到map中
                                                                                                 for (HomePageTrendInfo nowInfo : now) {
                                                                                                     if(day<=1) {
                                                                                                         String date = nowInfo.getDate();
                                                                                                         Date parse =null;
                                                                                                         try {
                                                                                                             parse = dateFormat.parse(date);
                                                                                                         } catch (ParseException e) {
                                                                                                             e.printStackTrace();
                                                                                                         }
                                                                                                         //增加一个小时
                                                                                                         Date addHourDate = DateUtil.addHour(parse, 1);
                                                                                                         String date1 = DateUtil.dateToString(addHourDate, DateUtil.DATE_WITHHOUR_FORMAT);
                                                                                                         if(addHourDate.compareTo(new Date())>0){
                                                                                                             date1 = DateUtil.dateToString(new Date(), DateUtil.DATE_WITHMINUTE_FORMAT );

                                                                                                         }
                                                                                                         if(map.get(date1)!=null) map.get(date1).setNowEnergyConsume(nowInfo.getNumber());
                                                                                                     }else {
                                                                                                         if(map.get(nowInfo.getDate())!=null) map.get(nowInfo.getDate()).setNowEnergyConsume(nowInfo.getNumber());
                                                                                                     }
                                                                                                 }



                                                                                             //6 查询去年的数据
                                                                                             return queryHelper.select(sql,HomePageTrendInfo::new)
                                                                                                               .where(dsl->dsl.gte("gather_time",startTime-31536000000L)
                                                                                                                              .lte("gather_time",endTime-31536000000L)
                                                                                                                              .in("device_id",deviceIds))
                                                                                                               .fetch()
                                                                                                               .collectList()
                                                                                                               .flatMapMany(last -> {
                                                                                                                   //7 将去年的数据更新到map中
                                                                                                                   for (HomePageTrendInfo lastInfo : last) {
                                                                                                                       if(day<=1){
                                                                                                                           String date = lastInfo.getDate();
                                                                                                                           Date parse =null;
                                                                                                                           try {
                                                                                                                               parse = dateFormat.parse(date);
                                                                                                                           } catch (ParseException e) {
                                                                                                                               e.printStackTrace();
                                                                                                                           }
                                                                                                                           //增加一个小时
                                                                                                                           Date addHourDate = DateUtil.addHour(parse, 1);
                                                                                                                           String date1 = DateUtil.dateToString(addHourDate, DateUtil.DATE_WITHHOUR_FORMAT);
                                                                                                                           if(addHourDate.compareTo(DateUtil.stringToDate(DateUtil.addYear(new Date(),-1)))>0){
                                                                                                                               date1 = DateUtil.dateToString(DateUtil.stringToDate(DateUtil.addYear(new Date(),-1)), DateUtil.DATE_WITHMINUTE_FORMAT );
                                                                                                                           }
                                                                                                                           if(map.get(date1)!=null) map.get(date1).setNowEnergyConsume(lastInfo.getNumber());
                                                                                                                       }else {
                                                                                                                           if(map.get(lastInfo.getDate())!=null) map.get(lastInfo.getDate()).setNowEnergyConsume(lastInfo.getNumber());
                                                                                                                       }

                                                                                                                   }
                                                                                                                   //8 收集返回
                                                                                                                   List<HomePageTrendDetailRes> list = map
                                                                                                                       .values()
                                                                                                                       .stream()
                                                                                                                       .sorted(Comparator.comparing(HomePageTrendDetailRes::getTime))
                                                                                                                       .collect(Collectors.toList());
                                                                                                                   return Flux.just(list);
                                                                                                               });
                                                                                         })
                                               )
                              );
    }








    @Operation(summary = "12月能耗环比同比")
    @PostMapping("/get/month/list/{type}")
    public Flux<Object> getEnergyMonth(@PathVariable String type) {
        //1. 获取所在月份
        Instant instant = Instant.ofEpochMilli(System.currentTimeMillis());
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("Asia/Shanghai"));
        LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();
        Integer endMonth = localDateTime.getMonth().getValue();
        //2. 构造SQL
        String mainSql= "SELECT\n" +
            "  MONTH(FROM_UNIXTIME(ROUND(gather_time / 1000,0))) AS month,\n" +
            "  SUM(difference) AS energyConsume,\n" +
            "  ROUND(SUM(difference * unit_price),2) AS expense\n" +
            "FROM\n" +
            "   sems_"+type+"_consume wc\n" +
            "WHERE \n" +
            "  YEAR(FROM_UNIXTIME(ROUND(gather_time / 1000,0))) = YEAR(CURRENT_DATE())";
        //3. 分组条件
        String group ="\nGROUP BY" +
                      "  MONTH(FROM_UNIXTIME(ROUND(gather_time / 1000,0)))\n" +
                      "ORDER BY\n" +
                      "  month;";
        return Flux.range(1,12).collectList().flatMapMany(list -> {
            //4. 初始化存放数据的map
            Map<Integer, HomePageEnergyMonthRes> map = new HashMap<Integer,HomePageEnergyMonthRes>();
            for (Integer month : list) {
                HomePageEnergyMonthRes homePageEnergyMonthRes= new HomePageEnergyMonthRes(month);
                map.put(month,homePageEnergyMonthRes);
            }
            return areaInfoService.getAllAreaDeviceIds(type)
                           .collectList()
                           .flatMapMany(deviceIds ->
                                        //5. 查询今年每月的能耗和价格更新到map中
                                        queryHelper.select(mainSql+group,HomePageEnergyMonthInfo::new)
                                                          .where(dsl -> dsl.in("device_id",deviceIds))
                                                          .fetch()
                                                          .collectList()
                                                          .flatMapMany(now -> {
                                                              if(now.size()>0){
                                                                  now.forEach(nowInfo -> {
                                                                      if(nowInfo.getEnergyConsume()!=null) map.get(nowInfo.getMonth()).setNowEnergyConsume(nowInfo.getEnergyConsume());
                                                                      if(nowInfo.getExpense()!=null) map.get(nowInfo.getMonth()).setExpense(nowInfo.getExpense());
                                                                  });
                                                              }
                                                              //6. 查询去年每月的能耗更新到map中
                                                              return queryHelper.select(mainSql+"-1"+group,HomePageEnergyMonthInfo::new)
                                                                                .where(dsl -> dsl.in("device_id",deviceIds))
                                                                                .fetch()
                                                                                .collectList()
                                                                                .flatMapMany(last -> {
                                                                                    if(last.size()>0){
                                                                                        last.forEach(lastInfo -> {
                                                                                            if(lastInfo.getEnergyConsume()!=null)
                                                                                                map.get(lastInfo.getMonth()).setLastEnergyConsume(lastInfo.getEnergyConsume());
                                                                                        });
                                                                                    }
                                                                                    //7. 上月能耗：将去年1月和今年1-11月依次放入map中,
                                                                                    // 置本年一月的上一个月能耗为去年最后一月的能耗
                                                                                    map.get(1).setLastMonthEnergyConsume(map.get(12).getLastEnergyConsume());
                                                                                    Integer i=2;
                                                                                    while (i<=endMonth){
                                                                                        map.get(i).setLastMonthEnergyConsume(map.get(i-1).getNowEnergyConsume());
                                                                                        i++;
                                                                                    }
                                                                                    //8. 计算同比环比
                                                                                    Collection<HomePageEnergyMonthRes> values = calculateRatio(map);
                                                                                    return Flux.just(values);
                                                                                });
                                                          })
                           );
                });
    }

    @Operation(summary = "查询设备当前状态")
    @Authorize(ignore = true)
    @GetMapping("/device/current/info")
    public Mono<DeviceCurrentInfoRes> queryDeviceCurrentInfo(String deviceId){
        Long startTime = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.of("+8")).toEpochMilli();
        Long endTime = System.currentTimeMillis();
        DeviceCurrentInfoRes deviceCurrentInfoRes = new DeviceCurrentInfoRes();
        return deviceService.createQuery()
                            .where(DeviceInfoEntity::getDeviceId,deviceId)
                            .and(DeviceInfoEntity::getStatus,"eq","0")
                            .fetchOne()
                            .flatMap(deviceInfoEntity -> {
                                deviceCurrentInfoRes.setDeviceName(deviceInfoEntity.getDeviceName());
                                deviceCurrentInfoRes.setComputeStatus(deviceInfoEntity.getComputeStatus());
                                return queryHelper
                                    .select("SELECT \n" +
                                        "*\n" +
                                        "FROM sems_electricity_consume\n" +
                                        "WHERE \n" +
                                        "device_id = ? AND gather_time BETWEEN ? AND ? \n" +
                                        "ORDER BY gather_time desc", ElectricityConsumeEntity::new, deviceId, startTime,endTime)
                                    .fetch()
                                    .collectList()
                                    .flatMap(list ->{
                                        BigDecimal difference = list.stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add);
                                        deviceCurrentInfoRes.setDifference(difference);
                                        if(!list.isEmpty()){
                                            deviceCurrentInfoRes.setPower(list.get(0).getPower());
                                        }
                                        AtomicReference<BigDecimal> currentTime = new AtomicReference<>(BigDecimal.ZERO);
                                        long timestamp =startTime;
                                        return queryHelper
                                            .select("SELECT \n" +
                                                "* \n" +
                                                "FROM `sems_device_state` \n" +
                                                "WHERE device_id = ? \n" +
                                                "AND ( start_time BETWEEN ? AND ?\n" +
                                                "OR end_time BETWEEN ? AND ? )\n" +
                                                "ORDER BY compute_status DESC", DeviceStateEntity::new, deviceId,startTime,endTime,startTime,endTime)
                                            .fetch()
                                            .switchIfEmpty(Flux.just(new DeviceStateEntity()))
                                            .flatMap(stateEntity -> {
                                                if (deviceCurrentInfoRes.getComputeStatus().equals("3") && Objects.isNull(stateEntity.getDeviceId())) {
                                                    currentTime.set(BigDecimal.valueOf(System.currentTimeMillis() - timestamp));
                                                }
                                                if(Objects.nonNull(stateEntity.getComputeStatus())){
                                                    if (stateEntity.getComputeStatus().equals("4") && stateEntity.getStartTime() < timestamp) {
                                                        currentTime.set(currentTime.get().add(BigDecimal.valueOf(stateEntity.getEndTime() - timestamp)));
                                                    }
                                                    if (stateEntity.getComputeStatus().equals("4") && stateEntity.getStartTime() >= timestamp) {
                                                        currentTime.set(currentTime.get().add(BigDecimal.valueOf(stateEntity.getEndTime() - stateEntity.getStartTime())));
                                                    }
                                                    if (stateEntity.getComputeStatus().equals("3")) {
                                                        currentTime.set(currentTime.get().add(BigDecimal.valueOf(System.currentTimeMillis() - stateEntity.getStartTime())));
                                                    }
                                                }
                                                deviceCurrentInfoRes.setRunTime(currentTime.get().divide(BigDecimal.valueOf(3600000), 2, RoundingMode.HALF_UP));
                                                return Mono.just(deviceCurrentInfoRes);
                                            })
                                            .collectMap(DeviceCurrentInfoRes::getDeviceName)
                                            .flatMap(e-> Mono.just(deviceCurrentInfoRes));
                                    });
                            });
    }

    /**计算12月能耗同比环比
    *<pre>{@code
    *
    *}</pre>
    * @param
    * @return
    * @see
    */
    public static Collection<HomePageEnergyMonthRes> calculateRatio(Map<Integer, HomePageEnergyMonthRes> map){
        Collection<HomePageEnergyMonthRes> values = map.values();
        for (HomePageEnergyMonthRes value : values) {
            //1.空值处理
            if(value.getNowEnergyConsume()==null) value.setNowEnergyConsume(BigDecimal.ZERO) ;
            if(value.getLastEnergyConsume()==null) value.setLastEnergyConsume(BigDecimal.ZERO);
            if(value.getLastMonthEnergyConsume()==null) value.setLastMonthEnergyConsume(BigDecimal.ZERO);

            BigDecimal yearOverYear = value.getNowEnergyConsume().subtract(value.getLastEnergyConsume());
            BigDecimal monthOverMonth = value.getNowEnergyConsume().subtract(value.getLastMonthEnergyConsume());

            //2.计算同比
            if(value.getLastEnergyConsume().compareTo(BigDecimal.ZERO)==0){
                if(value.getNowEnergyConsume().compareTo(BigDecimal.ZERO)==0) yearOverYear=BigDecimal.ZERO;
                else yearOverYear = BigDecimal.valueOf(100);
            }
            else yearOverYear = yearOverYear.divide(value.getLastEnergyConsume(),BigDecimal.ROUND_CEILING).multiply(BigDecimal.valueOf(100));


            //3.计算环比
            if(value.getLastMonthEnergyConsume().compareTo(BigDecimal.ZERO)==0){
                monthOverMonth=BigDecimal.ZERO;
                if(value.getNowEnergyConsume().compareTo(BigDecimal.ZERO)==0) yearOverYear=BigDecimal.ZERO;
            }
            else monthOverMonth = monthOverMonth.divide(value.getLastMonthEnergyConsume(),BigDecimal.ROUND_CEILING).multiply(BigDecimal.valueOf(100));


            value.setYearOverYearRatio(yearOverYear);
            value.setMonthOverMonthRatio(monthOverMonth);
        }
        return values;
    }



    /**判断能耗趋势时间维度
     *<pre>{@code
     *
     *}</pre>
     * @param
     * @return  开始时间
     * @see
     */
    public static void getTimeDimension(HomePageDto homePageDto,Integer dateType,Map<String,HomePageTrendInfo> timeMap) {
        long currentTime = System.currentTimeMillis();
        homePageDto.setEndTime(currentTime);
        //日
        if(dateType==1) {
            homePageDto.setDimension("%Y-%m-%d %H:00");
            homePageDto.setStartTime(DateUtil.getDailyStartTime(currentTime,"GMT+8:00"));
        }//周
        else if(dateType==2) {
            homePageDto.setDimension("%Y-%m-%d %H");
            homePageDto.setStartTime(DateUtil.getWeekStartTime());
        }//月
        else if(dateType==3) {
            homePageDto.setDimension("%Y-%m-%d");
            homePageDto.setStartTime(DateUtil.getMonthStartTime(currentTime,"GMT+8:00"));
        }//年
        else {
            homePageDto.setDimension("%Y-%m");
            homePageDto.setStartTime(DateUtil.getYearStartTime(currentTime,"GMT+8:00"));
        }
        LocalDateTime startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(homePageDto.getStartTime()), ZoneId.of("Asia/Shanghai"));
        LocalDateTime endDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(homePageDto.getEndTime()), ZoneId.of("Asia/Shanghai"));
        getTimeMap(homePageDto.getDimension(),startDateTime,endDateTime,timeMap);
    }

    /**判断能耗趋势时间维度
     *<pre>{@code
     *
     *}</pre>
     * @param
     * @return  维度格式字符串
     * @see
     */
    public static String getDetailDimension(Long startTime, Long endTime,Map<String,HomePageTrendDetailRes> timeMap) {
        String dimension;
        // 1 转换为LocalDateTime对象，使用上海时区
        LocalDateTime startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.of("Asia/Shanghai"));
        LocalDateTime endDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.of("Asia/Shanghai"));
        // 2 计算时间差，单位为天
        long daysDiff = ChronoUnit.DAYS.between(startDateTime, endDateTime);
        if (daysDiff < 7 ) {
            //3.1 小于一周以月日时为单位
            dimension = "%Y-%m-%d %H:00";

        } else if (daysDiff <= 90){
            //3.2 小于三个月以年月日为单位
            dimension = "%Y-%m-%d";

        }else {
            //3.3 大于三个月以月为单位
            dimension = "%Y-%m";
        }
        getDetailTimeMap(dimension,startDateTime,endDateTime,timeMap);
        return dimension;
    }




    /**计算查询实时能耗环比
    *<pre>{@code
    *
    *}</pre>
    * @param
    * @return
    * @see
    */
    public static HomePageRealTimeRes calculatePeriodOnPeriod(HomePageRealTimeRes last,HomePageRealTimeRes now){
        BigDecimal waterPeriodOnPeriod=now.getWaterEnergyConsume().subtract(last.getWaterEnergyConsume());
        BigDecimal electricityPeriodOnPeriod=now.getElectricityEnergyConsume().subtract(last.getElectricityEnergyConsume());
        BigDecimal gasPeriodOnPeriod=now.getGasEnergyConsume().subtract(last.getGasEnergyConsume());
        //1.计算水环比
        if(last.getWaterEnergyConsume().compareTo(BigDecimal.ZERO)==0){
            waterPeriodOnPeriod=BigDecimal.ZERO;
        }else waterPeriodOnPeriod=waterPeriodOnPeriod.divide(last.getWaterEnergyConsume(),BigDecimal.ROUND_CEILING).multiply(BigDecimal.valueOf(100));
        //2.计算电环比
        if(last.getElectricityEnergyConsume().compareTo(BigDecimal.ZERO)==0){
            electricityPeriodOnPeriod=BigDecimal.ZERO;
        }else electricityPeriodOnPeriod=electricityPeriodOnPeriod.divide(last.getElectricityEnergyConsume(),BigDecimal.ROUND_CEILING).multiply(BigDecimal.valueOf(100));
        //3.计算气环比
        if(last.getGasEnergyConsume().compareTo(BigDecimal.ZERO)==0){
            gasPeriodOnPeriod=BigDecimal.ZERO;
        }else gasPeriodOnPeriod=gasPeriodOnPeriod.divide(last.getGasEnergyConsume(),BigDecimal.ROUND_CEILING).multiply(BigDecimal.valueOf(100));

        now.setWaterPeriodOnPeriod(waterPeriodOnPeriod);
        now.setElectricityPeriodOnPeriod(electricityPeriodOnPeriod);
        now.setGasPeriodOnPeriod(gasPeriodOnPeriod);
        return now;
    }

    /**获取能耗趋势map
    *<pre>{@code
    *
    *}</pre>
    * @param
    * @return
    * @see
    */
    public static void getTimeMap(String dimension,LocalDateTime startDate, LocalDateTime endDate,Map<String,HomePageTrendInfo> map){
        List<String> timeList = DateUtil.getHoursBetweenDates(dimension, startDate, endDate);
        for (String time : timeList) {
            HomePageTrendInfo info = new HomePageTrendInfo();
            info.setDate(time);
            info.setNumber(BigDecimal.ZERO);
            map.put(time,info);
        }
        timeList.clear();
    }

    /**获取能耗趋势详情map
    *<pre>{@code
    *
    *}</pre>
    * @param
    * @return
    * @see
    */
    public static void getDetailTimeMap(String dimension,LocalDateTime startDate, LocalDateTime endDate,Map<String,HomePageTrendDetailRes> map){
        List<String> timeList = DateUtil.getHoursBetweenDates(dimension, startDate, endDate);
        for (String time : timeList) {
            HomePageTrendDetailRes info = new HomePageTrendDetailRes();
            info.setTime(time);
            info.setNowEnergyConsume(BigDecimal.ZERO);
            info.setLastEnergyConsume(BigDecimal.ZERO);
            map.put(time,info);
        }
        timeList.clear();
    }






}
