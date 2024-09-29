package org.jetlinks.pro.sems.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.sems.entity.res.EnergyForecastCurMonthRes;
import org.jetlinks.pro.sems.entity.res.EnergyForecastTrendInfo;
import org.jetlinks.pro.sems.entity.res.EnergyForecastTrendRes;
import org.jetlinks.pro.sems.entity.res.TestRecordEnergyRes;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.jetlinks.pro.sems.entity.CostConfigEntity;
import org.jetlinks.pro.sems.entity.TestEnergyDetailEntity;
import org.jetlinks.pro.sems.entity.TestRecordEntity;
import org.jetlinks.pro.sems.service.AreaInfoService;
import org.jetlinks.pro.sems.service.CostConfService;
import org.jetlinks.pro.sems.service.TestEnergyDetailService;
import org.jetlinks.pro.sems.service.TestRecordService;
import org.jetlinks.pro.sems.utils.DateUtil;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.0
 */
@RestController
@RequestMapping("/sems/energy/forecast")
@AllArgsConstructor
@Getter
@Tag(name = "能耗预测1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "energy-forecast", name = "能耗预测")
public class EnergyConsumptionForecastController {
    private final ReactiveRedisTemplate<String, String> redis;
    private final QueryHelper queryHelper;
    private final AreaInfoService areaInfoService;
    private final CostConfService costConfService;
    private final TestRecordService testRecordService;
    private final TestEnergyDetailService testEnergyDetailService;

    @Operation(summary = "趋势预测")
    @PostMapping("/trend/{type}")
    @Authorize(ignore = true)
    public Flux<Object> trendForecast(@PathVariable String type) {
        LocalDate nowDate = LocalDate.now();
        long current = nowDate.atStartOfDay().toEpochSecond(ZoneOffset.of("+8"))*1000;
        long startTime = nowDate.minusMonths(1).atStartOfDay().toEpochSecond(ZoneOffset.of("+8"))*1000;
        long endTime = nowDate.plusMonths(1).atStartOfDay().toEpochSecond(ZoneOffset.of("+8"))*1000;
        Map<String, EnergyForecastTrendRes> map = new HashMap<>();
        getMap(startTime,endTime,map);
        String sql="SELECT\n" +
            "   FROM_UNIXTIME((gather_time) / 1000, '%Y-%m-%d') AS date,\n" +
            "    SUM(w.difference) AS number\n" +
            "FROM\n" +
            "    sems_"+type+"_consume w where w.device_id='0'\n" +
            "GROUP BY\n" +
            "    FROM_UNIXTIME((gather_time) / 1000, '%Y-%m-%d');";
        //2.查询总表前30天到今天的能耗
        return Flux.just(map)
                   .flatMap(dataMap ->
                                queryHelper
                                    .select(sql, EnergyForecastTrendInfo::new)
                                    .where(dsl->dsl.lte("gather_time",System.currentTimeMillis())
                                                   .gte("gather_time",startTime))
                                    .fetch()
                                    .collectList()
                                    .flatMapMany(currentInfos ->{
                                        //3.填装map
                                        if(currentInfos.size()>0){
                                            for (EnergyForecastTrendInfo currentInfo : currentInfos) {
                                                if(dataMap.get(currentInfo.getDate())!=null){
                                                    dataMap.get(currentInfo.getDate()).setCurrent(currentInfo.getNumber());
                                                }
                                            }
                                        }
                                        LocalDateTime startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(current+86400000L), ZoneId.of("Asia/Shanghai"));
                                        LocalDateTime endDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime+86400000L), ZoneId.of("Asia/Shanghai"));
                                        //4.获取未来30天的时间list
                                        List<String> dates = DateUtil.getHoursBetweenDates("%Y-%m-%d", startDateTime, endDateTime);
                                        List<String> list = dates
                                            .stream()
                                            .map(i -> "forecasted_" + type +":"+ i)
                                            .collect(Collectors.toList());
                                        //5.从redis获取未来30天的能耗
                                        return redis.opsForValue()
                                                    .multiGet(list)
                                                    .flatMapMany(i -> {
                                                        for (int j = 0; j < dates.size(); j++) {
                                                            String temp = i.get(j);
                                                            if(temp!=null){
                                                                String[] strings = temp.split(",");
                                                                map.get(dates.get(j)).setCurrent(BigDecimal.valueOf(Double.parseDouble(strings[0])).setScale(2, RoundingMode.UP));
                                                            }
                                                        }
                                                        //6.查询同比
                                                        return queryHelper.select(sql,EnergyForecastTrendInfo::new)
                                                                          .where(dsl->dsl.lte("gather_time",nowDate.plusMonths(1).minusYears(1).atStartOfDay().toEpochSecond(ZoneOffset.of("+8"))*1000)
                                                                                         .gte("gather_time",nowDate.minusMonths(1).minusYears(1).atStartOfDay().toEpochSecond(ZoneOffset.of("+8"))*1000))
                                                                          .fetch()
                                                                          .collectList()
                                                                          .flatMapMany(yearOnYear -> {
                                                                              if(yearOnYear.size()>0){
                                                                                  for (EnergyForecastTrendInfo currentInfo : yearOnYear) {
                                                                                      LocalDate localDate = LocalDate.parse(currentInfo.getDate());
                                                                                      String yearDay  = localDate.plusYears(1).toString();
                                                                                      if(dataMap.get(yearDay)!=null){
                                                                                          dataMap.get(yearDay).setYearOnYear(currentInfo.getNumber());
                                                                                      }
                                                                                      if(dataMap.get(currentInfo.getDate())!=null){
                                                                                          dataMap.get(currentInfo.getDate()).setCurrent(currentInfo.getNumber());
                                                                                      }
                                                                                  }
                                                                              }
                                                                              //7.查询环比
                                                                              return queryHelper.select(sql,EnergyForecastTrendInfo::new)
                                                                                                .where(dsl->dsl.lte("gather_time",nowDate.plusMonths(1).minusMonths(1).atStartOfDay().toEpochSecond(ZoneOffset.of("+8"))*1000)
                                                                                                               .gte("gather_time",nowDate.minusMonths(1).minusMonths(1).atStartOfDay().toEpochSecond(ZoneOffset.of("+8"))*1000))
                                                                                                .fetch()
                                                                                                .collectList()
                                                                                                .flatMapMany(monthOnMonth -> {
                                                                                                    if (monthOnMonth.size() > 0) {
                                                                                                        for (EnergyForecastTrendInfo currentInfo : monthOnMonth) {
                                                                                                            LocalDate localDate = LocalDate.parse(currentInfo.getDate());
                                                                                                            String monthDay  = localDate.plusMonths(1).toString();
                                                                                                            if (dataMap.get(monthDay) != null) {
                                                                                                                dataMap.get(monthDay).setMonthOnMonth(currentInfo.getNumber());
                                                                                                            }
                                                                                                            if (dataMap.get(currentInfo.getDate()) != null) {
                                                                                                                dataMap.get(currentInfo.getDate()).setCurrent(currentInfo.getNumber());
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                    List<EnergyForecastTrendRes> res = map
                                                                                                        .values()
                                                                                                        .stream()
                                                                                                        .sorted(Comparator
                                                                                                                    .comparing(EnergyForecastTrendRes::getDate))
                                                                                                        .collect(Collectors.toList());
                                                                                                    for (EnergyForecastTrendRes re : res) {
                                                                                                        //8计算同比环比
                                                                                                        re.setYearOnYearRatio(getMonthOnMonth(re.getYearOnYear(),re.getCurrent()));
                                                                                                        re.setMonthOnMontRatio(getMonthOnMonth(re.getMonthOnMonth(),re.getCurrent()));
                                                                                                    }
                                                                                                    return Flux.fromIterable(res);
                                                                                                }); });
                                                    });
                                    })
                   );

    }

    @Operation(summary = "试验能耗趋势预测")
    @PostMapping("/testTrend/{type}")
    @Authorize(ignore = true)
    public Flux<Object> testTrendForecast(@PathVariable String type) {
        LocalDate nowDate = LocalDate.now();
        long current = nowDate.atStartOfDay().toEpochSecond(ZoneOffset.of("+8"))*1000;
        long startTime = nowDate.minusMonths(1).atStartOfDay().toEpochSecond(ZoneOffset.of("+8"))*1000;
        long endTime = nowDate.plusMonths(1).atStartOfDay().toEpochSecond(ZoneOffset.of("+8"))*1000;
        Map<String,EnergyForecastTrendRes> map = new HashMap<>();
        getMap(startTime,endTime,map);
        String sql="SELECT\n" +
            "   FROM_UNIXTIME((gather_time) / 1000, '%Y-%m-%d') AS date,\n" +
            "    SUM(w.difference) AS number\n" +
            "FROM\n" +
            "    sems_"+type+"_consume w where w.device_id='0'\n" +
            "GROUP BY\n" +
            "    FROM_UNIXTIME((gather_time) / 1000, '%Y-%m-%d');";
        //2.查询总表前30天到今天的能耗
        return Flux.just(map)
            .flatMap(dataMap ->
                queryHelper
                    .select(sql, EnergyForecastTrendInfo::new)
                    .where(dsl->dsl.lte("gather_time",System.currentTimeMillis())
                        .gte("gather_time",startTime))
                    .fetch()
                    .collectList()
                    .flatMapMany(currentInfos ->{
                        //3.填装map
                        if(currentInfos.size()>0){
                            for (EnergyForecastTrendInfo currentInfo : currentInfos) {
                                if(dataMap.get(currentInfo.getDate())!=null){
                                    dataMap.get(currentInfo.getDate()).setCurrent(currentInfo.getNumber());
                                }
                            }
                        }
                        LocalDateTime startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(current+86400000L), ZoneId.of("Asia/Shanghai"));
                        LocalDateTime endDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime+86400000L), ZoneId.of("Asia/Shanghai"));
                        //4.获取未来30天的时间list
                        List<String> dates = DateUtil.getHoursBetweenDates("%Y-%m-%d", startDateTime, endDateTime);
                        //填充预测数据

                            return this.estimateTest(type,dates,dataMap)
                                .thenMany(
                                    //6.查询同比
                         queryHelper.select(sql,EnergyForecastTrendInfo::new)
                            .where(dsl->dsl.lte("gather_time",nowDate.plusMonths(1).minusYears(1).atStartOfDay().toEpochSecond(ZoneOffset.of("+8"))*1000)
                                .gte("gather_time",nowDate.minusMonths(1).minusYears(1).atStartOfDay().toEpochSecond(ZoneOffset.of("+8"))*1000))
                            .fetch()
                            .collectList()
                            .flatMapMany(yearOnYear -> {
                                if(yearOnYear.size()>0){
                                    for (EnergyForecastTrendInfo currentInfo : yearOnYear) {
                                        LocalDate localDate = LocalDate.parse(currentInfo.getDate());
                                        String yearDay  = localDate.plusYears(1).toString();
                                        if(dataMap.get(yearDay)!=null){
                                            dataMap.get(yearDay).setYearOnYear(currentInfo.getNumber());
                                        }
                                        if(dataMap.get(currentInfo.getDate())!=null){
                                            dataMap.get(currentInfo.getDate()).setCurrent(currentInfo.getNumber());
                                        }
                                    }
                                }
                                //7.查询环比
                                return queryHelper.select(sql,EnergyForecastTrendInfo::new)
                                    .where(dsl->dsl.lte("gather_time",nowDate.plusMonths(1).minusMonths(1).atStartOfDay().toEpochSecond(ZoneOffset.of("+8"))*1000)
                                        .gte("gather_time",nowDate.minusMonths(1).minusMonths(1).atStartOfDay().toEpochSecond(ZoneOffset.of("+8"))*1000))
                                    .fetch()
                                    .collectList()
                                    .flatMapMany(monthOnMonth -> {
                                        if (monthOnMonth.size() > 0) {
                                            for (EnergyForecastTrendInfo currentInfo : monthOnMonth) {
                                                LocalDate localDate = LocalDate.parse(currentInfo.getDate());
                                                String monthDay  = localDate.plusMonths(1).toString();
                                                if (dataMap.get(monthDay) != null) {
                                                    dataMap.get(monthDay).setMonthOnMonth(currentInfo.getNumber());
                                                }
                                                if (dataMap.get(currentInfo.getDate()) != null) {
                                                    dataMap.get(currentInfo.getDate()).setCurrent(currentInfo.getNumber());
                                                }
                                            }
                                        }
                                        List<EnergyForecastTrendRes> res = map
                                            .values()
                                            .stream()
                                            .sorted(Comparator
                                                .comparing(EnergyForecastTrendRes::getDate))
                                            .collect(Collectors.toList());
                                        for (EnergyForecastTrendRes re : res) {
                                            //8计算同比环比
                                            re.setYearOnYearRatio(getMonthOnMonth(re.getYearOnYear(),re.getCurrent()));
                                            re.setMonthOnMontRatio(getMonthOnMonth(re.getMonthOnMonth(),re.getCurrent()));
                                        }
                                        return Flux.fromIterable(res);
                                    });
                            })
                                );


                    })
            );

    }


    @Operation(summary = "用量预测")
    @PostMapping("/amount/{type}")
    @QueryAction
    public synchronized Flux<Object> amountForecast(@PathVariable String type) {
        long current = System.currentTimeMillis();
        long startTime = DateUtil.getMonthStartTime(current,"GMT+8:00");
        long endTime = DateUtil.getMonthEndTime(current,"GMT+8:00");
        long lastMonthCurrent = DateUtil.getLastMonthCurrent();
        long lastStartTime = DateUtil.getMonthStartTime(startTime-10000L,"GMT+8:00");
        long lastEndTime = DateUtil.getMonthEndTime(lastMonthCurrent,"GMT+8:00");
        String sql="SELECT\n" +
            "    SUM(w.difference) AS curMonthNumber,\n" +
            "    ROUND(SUM(difference * unit_price),2) AS forMonthExpense\n" +
            "FROM\n" +
            "    sems_"+type+"_consume w where w.device_id='0';";
        //1.查询所有区域所有设备//2.查询这个月的费用
        return queryHelper
            .select(sql, EnergyForecastCurMonthRes::new)
            .where(dsl->dsl.lte("gather_time",current)
                           .gte("gather_time",startTime))
            .fetch()
            .flatMap(currentInfo -> {
                if(currentInfo.getCurMonthNumber()==null) currentInfo.setCurMonthOnMonth(BigDecimal.ZERO);
                if(currentInfo.getCurMonthNumber()==null) currentInfo.setCurMonthNumber(BigDecimal.ZERO);
                if(currentInfo.getForMonthExpense()==null) currentInfo.setForMonthExpense(BigDecimal.ZERO);
                //3.查询上个月的费用
                return queryHelper.select(sql,EnergyForecastCurMonthRes::new)
                                  .where(dsl->dsl.lte("gather_time",lastEndTime)
                                                 .gte("gather_time",lastMonthCurrent))
                                  .fetch()
                                  .flatMap(lastInfo ->{
                                      if(lastInfo.getCurMonthNumber()==null) lastInfo.setCurMonthNumber(BigDecimal.ZERO);
                                      if(lastInfo.getCurMonthNumber()==null) lastInfo.setCurMonthOnMonth(BigDecimal.ZERO);
                                      currentInfo.setCurMonthOnMonth(getMonthOnMonth(lastInfo.getCurMonthNumber(),currentInfo.getCurMonthNumber()));
                                      LocalDateTime startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(current), ZoneId.of("Asia/Shanghai"));
                                      LocalDateTime endDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.of("Asia/Shanghai"));
                                      List<String> dates = DateUtil.getHoursBetweenDates("%Y-%m-%d", startDateTime, endDateTime);
                                      List<String> list = dates
                                          .stream()
                                          .map(i -> "forecasted_" + type +":"+ i)
                                          .collect(Collectors.toList());
                                      //4.查询预测费用
                                      return redis.opsForValue()
                                                  .multiGet(list)
                                                  .flatMapMany(forecast -> {
                                                      BigDecimal forAmount = BigDecimal.ZERO;
                                                      if(currentInfo.getCurMonthNumber().compareTo(BigDecimal.ZERO)>0){
                                                          forAmount = currentInfo.getCurMonthNumber();
                                                      }
                                                      BigDecimal forExpenseAmount = currentInfo.getForMonthExpense();
                                                      for (String data : forecast) {
                                                          if(data!=null){
                                                              String[] datas = data.split(",");
                                                              if(datas!=null){
                                                                  forAmount = forAmount.add(BigDecimal.valueOf(Double.parseDouble(datas[0])).setScale(2, RoundingMode.UP));
                                                                  forExpenseAmount = forExpenseAmount.add(BigDecimal.valueOf(Double.parseDouble(datas[1])).setScale(2, RoundingMode.UP));
                                                              }
                                                          }
                                                      }
                                                      currentInfo.setForMonthNumber(forAmount);
                                                      String energyType = type.equals("gas") ? "3" : type.equals("water") ? "1" : "2" ;
                                                      BigDecimal finalForAmount = forAmount;
                                                      return costConfService.createQuery()
                                                                            .where(CostConfigEntity::getEnergyType,energyType)
                                                                            .and(CostConfigEntity::getState,"1")
                                                                            .orderBy(SortOrder.desc(CostConfigEntity::getModifyTime))
                                                                            .fetchOne()
                                                                            .flatMap(price ->{
                                                                                if(energyType.equals("2")){
                                                                                    currentInfo.setForMonthExpense(finalForAmount
                                                                                                                       .multiply(BigDecimal.valueOf(
                                                                                                                           Double.parseDouble(price.getReferencePrice())))
                                                                                                                       .setScale(2,RoundingMode.HALF_UP));
                                                                                } else {
                                                                                    currentInfo.setForMonthExpense(finalForAmount
                                                                                                                       .multiply(BigDecimal.valueOf(price.getUnitPrice()))
                                                                                                                       .setScale(2,RoundingMode.HALF_UP));
                                                                                }
                                                                                return Mono.just(currentInfo);
                                                                            });
                                                  })
                                                  .flatMap(e-> queryHelper
                                                      .select(sql, EnergyForecastCurMonthRes::new)
                                                      .where(dsl->dsl.lte("gather_time",lastEndTime)
                                                                     .gte("gather_time",lastStartTime))
                                                      .fetch()
                                                      .flatMap(lastMonth -> {
                                                          if(lastMonth.getCurMonthNumber()==null) lastMonth.setCurMonthNumber(BigDecimal.ZERO);
                                                          if(lastMonth.getForMonthExpense()==null) lastMonth.setForMonthExpense(BigDecimal.ZERO);
                                                          if(lastMonth.getCurMonthNumber()==null) currentInfo.setCurMonthOnMonth(BigDecimal.ZERO);
                                                          if(lastMonth.getForMonthExpense()==null) currentInfo.setForMonthExpense(BigDecimal.ZERO);
                                                          currentInfo.setForMonthOnMonth(getMonthOnMonth(lastMonth.getCurMonthNumber(),currentInfo.getForMonthNumber()));
                                                          currentInfo.setForMonthOnMonthExpense(getMonthOnMonth(lastMonth.getForMonthExpense(),currentInfo.getForMonthExpense()));
                                                          return Flux.just(currentInfo);
                                                      })
                                                  );
                                  });
            });
    }

    @Operation(summary = "用量预测按试验")
    @PostMapping("/testAmount/{type}")
    @QueryAction
    public synchronized Flux<Object> testAmountForecast(@PathVariable String type) {
        long current = System.currentTimeMillis();
        long startTime = DateUtil.getMonthStartTime(current,"GMT+8:00");
        long endTime = DateUtil.getMonthEndTime(current,"GMT+8:00");
        long lastMonthCurrent = DateUtil.getLastMonthCurrent();
        long lastStartTime = DateUtil.getMonthStartTime(startTime-10000L,"GMT+8:00");
        long lastEndTime = DateUtil.getMonthEndTime(lastMonthCurrent,"GMT+8:00");
        String sql="SELECT\n" +
            "    SUM(w.difference) AS curMonthNumber,\n" +
            "    ROUND(SUM(difference * unit_price),2) AS forMonthExpense\n" +
            "FROM\n" +
            "    sems_"+type+"_consume w where w.device_id='0';";
        //1.查询所有区域所有设备//2.查询这个月的费用
        return queryHelper
            .select(sql,EnergyForecastCurMonthRes::new)
            .where(dsl->dsl.lte("gather_time",current)
                .gte("gather_time",startTime))
            .fetch()
            .flatMap(currentInfo -> {
                if(currentInfo.getCurMonthNumber()==null) currentInfo.setCurMonthOnMonth(BigDecimal.ZERO);
                if(currentInfo.getCurMonthNumber()==null) currentInfo.setCurMonthNumber(BigDecimal.ZERO);
                if(currentInfo.getForMonthExpense()==null) currentInfo.setForMonthExpense(BigDecimal.ZERO);
                //3.查询上个月的费用
                return queryHelper.select(sql,EnergyForecastCurMonthRes::new)
                    .where(dsl->dsl.lte("gather_time",lastEndTime)
                        .gte("gather_time",lastMonthCurrent))
                    .fetch()
                    .flatMap(lastInfo ->{
                        if(lastInfo.getCurMonthNumber()==null) lastInfo.setCurMonthNumber(BigDecimal.ZERO);
                        if(lastInfo.getCurMonthNumber()==null) lastInfo.setCurMonthOnMonth(BigDecimal.ZERO);
                        currentInfo.setCurMonthOnMonth(getMonthOnMonth(lastInfo.getCurMonthNumber(),currentInfo.getCurMonthNumber()));
                        LocalDateTime startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(current), ZoneId.of("Asia/Shanghai"));
                        LocalDateTime endDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.of("Asia/Shanghai"));
                        List<String> dates = DateUtil.getHoursBetweenDates("%Y-%m-%d", startDateTime, endDateTime);

                        //查询预测费用
                        return this.estimateTestCost(type,dates)
                                .flatMapMany(va-> {
                                    BigDecimal number = va.add(currentInfo.getCurMonthNumber() == null ? BigDecimal.ZERO : currentInfo.getCurMonthNumber());
                                    currentInfo.setForMonthNumber(number);
                                    String energyType = type.equals("gas") ? "3" : type.equals("water") ? "1" : "2";
                                    BigDecimal finalForAmount = number;
                                    return costConfService.createQuery()
                                        .where(CostConfigEntity::getEnergyType, energyType)
                                        .and(CostConfigEntity::getState, "1")
                                        .orderBy(SortOrder.desc(CostConfigEntity::getModifyTime))
                                        .fetchOne()
                                        .flatMap(price -> {
                                            if (energyType.equals("2")) {
                                                currentInfo.setForMonthExpense(finalForAmount
                                                    .multiply(BigDecimal.valueOf(
                                                        Double.parseDouble(price.getReferencePrice())))
                                                    .setScale(2, RoundingMode.HALF_UP));
                                            } else {
                                                currentInfo.setForMonthExpense(finalForAmount
                                                    .multiply(BigDecimal.valueOf(price.getUnitPrice()))
                                                    .setScale(2, RoundingMode.HALF_UP));
                                            }
                                            return Mono.just(currentInfo);
                                        });
                                })
                            .flatMap(e-> queryHelper
                                .select(sql, EnergyForecastCurMonthRes::new)
                                .where(dsl->dsl.lte("gather_time",lastEndTime)
                                    .gte("gather_time",lastStartTime))
                                .fetch()
                                .flatMap(lastMonth -> {
                                    if(lastMonth.getCurMonthNumber()==null) lastMonth.setCurMonthNumber(BigDecimal.ZERO);
                                    if(lastMonth.getForMonthExpense()==null) lastMonth.setForMonthExpense(BigDecimal.ZERO);
                                    if(lastMonth.getCurMonthNumber()==null) currentInfo.setCurMonthOnMonth(BigDecimal.ZERO);
                                    if(lastMonth.getForMonthExpense()==null) currentInfo.setForMonthExpense(BigDecimal.ZERO);
                                    currentInfo.setForMonthOnMonth(getMonthOnMonth(lastMonth.getCurMonthNumber(),currentInfo.getForMonthNumber()));
                                    currentInfo.setForMonthOnMonthExpense(getMonthOnMonth(lastMonth.getForMonthExpense(),currentInfo.getForMonthExpense()));
                                    return Flux.just(currentInfo);
                                })
                            );
                    });
            });
    }



    /**获得一个map的结果集
    *<pre>{@code
    *
    *}</pre>
    * @param
    * @return
    * @see
    */
    public static void getMap(long startTime, long endTime, Map<String,EnergyForecastTrendRes> map){
        LocalDateTime startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.of("Asia/Shanghai"));
        LocalDateTime endDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.of("Asia/Shanghai"));
        List<String> list = DateUtil.getHoursBetweenDates("%Y-%m-%d", startDateTime, endDateTime);
        for (String time : list) {
            EnergyForecastTrendRes info = new EnergyForecastTrendRes();
            info.setDate(time);
            info.setCurrent(BigDecimal.ZERO);
            info.setYearOnYear(BigDecimal.ZERO);
            info.setMonthOnMonth(BigDecimal.ZERO);
            map.put(time,info);
        }
    }

    /** 计算同比
    *<pre>{@code
    *
    *}</pre>
    * @param
    * @return
    * @see
    */
    public static BigDecimal getMonthOnMonth(BigDecimal last,BigDecimal now){
        if(last==null){
            last=BigDecimal.ZERO;
        }
        if(now==null){
            now=BigDecimal.ZERO;
        }
        BigDecimal temp = now.subtract(last);
        if(last.compareTo(BigDecimal.ZERO)==0){
            if(now.compareTo(BigDecimal.ZERO)==0)
                return BigDecimal.ZERO;
            else
                return BigDecimal.valueOf(100);
        }else {
            return temp.divide(last,BigDecimal.ROUND_CEILING).multiply(BigDecimal.valueOf(100));
        }
    }


    public Mono<Void> estimateTest(String type,List<String> dates ,Map<String,EnergyForecastTrendRes> map){

        //获取时间段内的预约数据
        return Flux.fromIterable(dates)
            .flatMap(va->{
                String startDate=va+" 00:00:00";
                long startTime = Objects.requireNonNull(DateUtil.stringToDate(startDate, DateUtil.DATE_WITHSECOND_FORMAT)).getTime();
                String endDate=va+" 23:59:59";
                long endTime = Objects.requireNonNull(DateUtil.stringToDate(endDate, DateUtil.DATE_WITHSECOND_FORMAT)).getTime();

                return queryHelper.select("SELECT * FROM sems_test_record WHERE record_type='1' and cancel_status='0' and not (test_start_time > " +endTime+ " or test_end_time < " + startTime+ ")",TestRecordEntity::new)
                    .fetch()
                    .collect(Collectors.groupingBy(TestRecordEntity::getConfigId))
                    .flatMap(ma->{
                        return this.getLastTest()
                            .collectList()
                            .flatMap(list->{
                                Map<String, TestRecordEnergyRes> collect = list.stream().collect(Collectors.toMap(TestRecordEnergyRes::getConfigId, Function.identity()));
                                HashMap<String, BigDecimal> resultMap = new HashMap<>();
                                BigDecimal  estimateElectricity =BigDecimal.ZERO;
                                BigDecimal estimateWater =BigDecimal.ZERO;
                                BigDecimal estimateGas =BigDecimal.ZERO;
                                for (Map.Entry<String, List<TestRecordEntity>> stringListEntry : ma.entrySet()) {
                                    if(collect.get(stringListEntry.getKey())!=null) {
                                        //计算能耗
                                        //时间段内做了多少次这个条目的试验
                                        int size = stringListEntry.getValue().size();
                                        TestRecordEnergyRes testRecordEnergyRes = collect.get(stringListEntry.getKey());
                                        //预估电能耗
                                        estimateElectricity = estimateElectricity.add(testRecordEnergyRes.getElectricityEnergy()==null?BigDecimal.ZERO:testRecordEnergyRes.getElectricityEnergy().multiply(BigDecimal.valueOf(size))) ;
                                        //预估水
                                        estimateWater = estimateWater.add(testRecordEnergyRes.getWaterEnergy()==null?BigDecimal.ZERO:testRecordEnergyRes.getWaterEnergy().multiply(BigDecimal.valueOf(size)));
                                        //预估气
                                        estimateGas = estimateGas.add(testRecordEnergyRes.getElectricityEnergy()==null?BigDecimal.ZERO:testRecordEnergyRes.getGasEnergy().multiply(BigDecimal.valueOf(size)));
                                    }
                                }
                                if("water".equals(type)) {
                                    if(map.get(va) != null){
                                        map.get(va).setCurrent(estimateWater);
                                    }

                                }else if("electricity".equals(type)) {
                                    if(map.get(va)!= null){
                                        map.get(va).setCurrent(estimateElectricity);
                                    }

                                }else {
                                    if(map.get(va) != null){
                                        map.get(va).setCurrent(estimateGas);
                                    }

                                }
                                return Mono.just(map);
                            });
                    });
            }).then();


    }

    public Mono<BigDecimal> estimateTestCost(String type,List<String> dates ){

        //获取时间段内的预约数据
        return Flux.fromIterable(dates)
            .flatMap(va->{
                String startDate=va+" 00:00:00";
                long startTime = Objects.requireNonNull(DateUtil.stringToDate(startDate, DateUtil.DATE_WITHSECOND_FORMAT)).getTime();
                String endDate=va+" 23:59:59";
                long endTime = Objects.requireNonNull(DateUtil.stringToDate(endDate, DateUtil.DATE_WITHSECOND_FORMAT)).getTime();

                return queryHelper.select("SELECT * FROM sems_test_record WHERE record_type='1' and cancel_status='0' and not (test_start_time > " +endTime+ " or test_end_time < " + startTime+ ")",TestRecordEntity::new)
                    .fetch()
                    .collect(Collectors.groupingBy(TestRecordEntity::getConfigId))
                    .flatMap(ma->{
                        return this.getLastTest()
                            .collectList()
                            .flatMap(list->{
                                Map<String, TestRecordEnergyRes> collect = list.stream().collect(Collectors.toMap(TestRecordEnergyRes::getConfigId, Function.identity()));
                                BigDecimal  estimateElectricityCost =BigDecimal.ZERO;
                                BigDecimal estimateWaterCost =BigDecimal.ZERO;
                                BigDecimal estimateGasCost =BigDecimal.ZERO;
                                for (Map.Entry<String, List<TestRecordEntity>> stringListEntry : ma.entrySet()) {
                                    if(collect.get(stringListEntry.getKey())!=null) {
                                        //计算能耗
                                        //时间段内做了多少次这个条目的试验
                                        int size = stringListEntry.getValue().size();
                                        TestRecordEnergyRes testRecordEnergyRes = collect.get(stringListEntry.getKey());
                                        //预估电费用
                                        estimateElectricityCost = estimateElectricityCost.add(testRecordEnergyRes.getElectricityCost()==null?BigDecimal.ZERO:testRecordEnergyRes.getElectricityCost().multiply(BigDecimal.valueOf(size))) ;
                                        //预估水费用
                                        estimateWaterCost = estimateWaterCost.add(testRecordEnergyRes.getWaterCost()==null?BigDecimal.ZERO:testRecordEnergyRes.getWaterCost().multiply(BigDecimal.valueOf(size)));
                                        //预估气费用
                                        estimateGasCost = estimateGasCost.add(testRecordEnergyRes.getElectricityCost()==null?BigDecimal.ZERO:testRecordEnergyRes.getGasCost().multiply(BigDecimal.valueOf(size)));
                                    }
                                }
                                if("water".equals(type)) {
                                    return Mono.just(estimateWaterCost);

                                }else if("electricity".equals(type)) {
                                    return Mono.just(estimateElectricityCost);

                                }else {
                                    return Mono.just(estimateGasCost);

                                }
                            });
                    });
            }).reduce(BigDecimal.ZERO,BigDecimal::add);
    }





    /**
     * 获取过去各条目的能耗
     * @return
     */
    public Flux<TestRecordEnergyRes> getLastTest(){
        //1.获取过去所有试验记录的能耗
        return testRecordService
            .createQuery()
            .where(TestRecordEntity::getRecordType,"0")
            .fetch()
            .flatMap(record->{
                TestRecordEnergyRes testRecordEnergyRes = new TestRecordEnergyRes();
                testRecordEnergyRes.setRecordId(record.getId());
                testRecordEnergyRes.setConfigId(record.getConfigId());
                testRecordEnergyRes.setConfigName(record.getTestName());
                return testEnergyDetailService
                    .createQuery()
                    .where(TestEnergyDetailEntity::getTestRecordId,record.getId())
                    .fetch()
                    .collectList()
                    .flatMapMany(list->{
                        //电能耗
                        BigDecimal electricityEnergy = list.stream().map(TestEnergyDetailEntity::getElectricity).reduce(BigDecimal.ZERO, BigDecimal::add);
                        testRecordEnergyRes.setElectricityEnergy(electricityEnergy);
                        //费用
                        BigDecimal electricityCost = list.stream().map(TestEnergyDetailEntity::getElectricityPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                        testRecordEnergyRes.setElectricityCost(electricityCost);

                        //水能耗
                        BigDecimal waterEnergy = list.stream().map(TestEnergyDetailEntity::getWater).reduce(BigDecimal.ZERO, BigDecimal::add);
                        testRecordEnergyRes.setWaterEnergy(waterEnergy);
                        //费用
                        BigDecimal waterCost = list.stream().map(TestEnergyDetailEntity::getWaterPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                        testRecordEnergyRes.setWaterCost(waterCost);

                        //气能耗
                        BigDecimal gasEnergy = list.stream().map(TestEnergyDetailEntity::getGas).reduce(BigDecimal.ZERO, BigDecimal::add);
                        testRecordEnergyRes.setGasEnergy(gasEnergy);
                        //费用
                        BigDecimal gasCost = list.stream().map(TestEnergyDetailEntity::getGasPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                        testRecordEnergyRes.setGasCost(gasCost);

                        return Mono.just(testRecordEnergyRes);
                    });

            }).collectList()
            .flatMapMany(value->{
                //按条目分组
                TestRecordEnergyRes testRecordEnergyRes ;
                ArrayList<TestRecordEnergyRes> result = new ArrayList<>();

                Map<String, List<TestRecordEnergyRes>> collectMapByItem = value.stream().collect(Collectors.groupingBy(TestRecordEnergyRes::getConfigId));
                for (Map.Entry<String, List<TestRecordEnergyRes>> stringListEntry : collectMapByItem.entrySet()) {
                    testRecordEnergyRes = new TestRecordEnergyRes();
                    testRecordEnergyRes.setConfigId(stringListEntry.getKey());
                    int size = stringListEntry.getValue().size();
                    BigDecimal lastTotalElectricity = stringListEntry.getValue().stream().map(TestRecordEnergyRes::getElectricityEnergy).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal lastTotalElectricityCost = stringListEntry.getValue().stream().map(TestRecordEnergyRes::getElectricityCost).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal lastTotalWater = stringListEntry.getValue().stream().map(TestRecordEnergyRes::getWaterEnergy).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal lastTotalWaterCost = stringListEntry.getValue().stream().map(TestRecordEnergyRes::getWaterCost).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal lastTotalGas = stringListEntry.getValue().stream().map(TestRecordEnergyRes::getGasEnergy).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal lastTotalGasCost = stringListEntry.getValue().stream().map(TestRecordEnergyRes::getGasCost).reduce(BigDecimal.ZERO, BigDecimal::add);

                    testRecordEnergyRes.setElectricityEnergy(lastTotalElectricity.divide(BigDecimal.valueOf(size),2, RoundingMode.HALF_UP));
                    testRecordEnergyRes.setElectricityCost(lastTotalElectricityCost.divide(BigDecimal.valueOf(size),2, RoundingMode.HALF_UP));
                    testRecordEnergyRes.setWaterEnergy(lastTotalWater.divide(BigDecimal.valueOf(size),2, RoundingMode.HALF_UP));
                    testRecordEnergyRes.setWaterCost(lastTotalWaterCost.divide(BigDecimal.valueOf(size),2, RoundingMode.HALF_UP));
                    testRecordEnergyRes.setGasEnergy(lastTotalGas.divide(BigDecimal.valueOf(size),2, RoundingMode.HALF_UP));
                    testRecordEnergyRes.setGasCost(lastTotalGasCost.divide(BigDecimal.valueOf(size),2, RoundingMode.HALF_UP));
                    result.add(testRecordEnergyRes);
                }
                return Flux.fromIterable(result);
            });
    }


}


