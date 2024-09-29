package org.jetlinks.project.busi.strategy.peak.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import lombok.RequiredArgsConstructor;
import org.apache.poi.util.StringUtil;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.project.busi.entity.*;
import org.jetlinks.project.busi.entity.res.*;
import org.jetlinks.project.busi.service.*;
import org.jetlinks.project.busi.strategy.peak.PeakAnalysisStrategy;
import org.jetlinks.project.busi.enums.TimeEnum;
import org.jetlinks.project.busi.strategy.peak.common.TimeHandler;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @ClassName WaterStrategy
 * @Author hky
 * @Time 2023/7/12 17:03
 * @Description 水
 **/
@Component("water")
@RequiredArgsConstructor
public class WaterStrategy implements PeakAnalysisStrategy {

    private final WaterConsumeService waterConsumeService;

    private final QueryHelper queryHelper;

    private final AreaInfoService areaInfoService;

    private final DeviceService deviceService;

    private final TestConfigDeviceService testConfigDeviceService;

    private final TestRecordService testRecordService;

    private final TestConfigService testConfigService;





    @Override
    public Mono<PeakAnalysisRes> getPeakDetail(QueryParamEntity query,TimeEnum timeEnum) {
        return getPeakAnalysisData(query,timeEnum.name());
    }

    @Override
    public Flux<PeakUniversalData> getPeakData(QueryParamEntity query) {
        query.setPaging(false);
        return waterConsumeService
            .query(query)
            .filter(i -> i.getDifference() != null && i.getCostId() != null)
            .map(PeakUniversalData::new);
    }

    @Override
    public Mono<List<CostRes>> getRegionPeakData(QueryParamEntity query) {
        String startDate = query.getContext().get("startDate").toString();
        String endDate = query.getContext().get("endDate").toString();

        return areaInfoService
            .createQuery()
            .where(AreaInfoEntity::getParentId, "0")
            .where(AreaInfoEntity::getState,"0")
            .fetch()
            .flatMap(areaInfo -> {
                return this.getEnergyByAreaIdSelf(areaInfo.getId(),areaInfo.getAreaName(),1,Long.valueOf(startDate),Long.valueOf(endDate));
            })
            .collectList()
            .flatMap(list->{
                List<CostRes> collect = list.stream().sorted(Comparator.comparing(CostRes::getRegion)).collect(Collectors.toList());
                return Mono.just(collect);
            });
    }

    @Override
    public Mono<Map<String,Object>> getRegionYearAnalysis(QueryParamEntity queryParam) {

        String year = queryParam.getContext().get("year").toString();
        String s = JSONArray.toJSONString(queryParam.getContext().get("nameAndIds"));
        List<ReturnTypeRes> nameAndIds = JSON.parseArray(s, ReturnTypeRes.class);
        HashMap<String, Object> resultMap = new HashMap<>();
        //指定年份的开始时间
        Year yearTime = Year.of(Integer.parseInt(year));
        LocalDate localDate = yearTime.atDay(1);
        LocalDateTime startOfDay = localDate.atStartOfDay();
        //指定年份的结束时间
        YearMonth yearMonth = YearMonth.of(Integer.parseInt(year), 12);
        LocalDate endOYear = yearMonth.atEndOfMonth();
        LocalDateTime endOfDay = endOYear.atTime(23, 59, 59, 999);

        ArrayList<TimeCostRes> timeCostRes2 = new ArrayList<>();
        return Flux.fromIterable(nameAndIds)
            .flatMap(nameAndId->{
                return this.getEnergyByAreaId(nameAndId.getId(),nameAndId.getName(),1,startOfDay.toInstant(ZoneOffset.of("+8")).toEpochMilli(),endOfDay.toInstant(ZoneOffset.of("+8")).toEpochMilli())
                    .flatMap(list->{
                        timeCostRes2.addAll(list);
                        return Mono.just(timeCostRes2);
                    });
            })
            .then(Mono.just(timeCostRes2))
            .flatMapMany(i->{

                Map<String, List<TimeCostRes>> groupByTime = i.stream().filter(item->StringUtil.isNotBlank(item.getGatherTime())).collect(Collectors.groupingBy(TimeCostRes::getGatherTime));

                return Flux.range(1,12)
                    .flatMap(month->{
                        String key=null;
                        if(month<10){
                             key= year+"-0"+month;
                        }else {
                             key= year+"-"+month;
                        }

                        ArrayList<TimeCostRes> timeCostRes = new ArrayList<>();
                    if(groupByTime.get(key) == null){
                        for (ReturnTypeRes nameAndId : nameAndIds) {
                            TimeCostRes timeCostRes1 = new TimeCostRes();
                            timeCostRes1.setAreaName(nameAndId.getName());
                            timeCostRes1.setCost(BigDecimal.ZERO);
                            timeCostRes.add(timeCostRes1);
                        }
                        resultMap.put(key,timeCostRes);
                        return Mono.just(resultMap);

                    }else {
                        List<TimeCostRes> energyCountRes = groupByTime.get(key);
                        Map<String, List<TimeCostRes>> collect = energyCountRes.stream().filter(item->StringUtil.isNotBlank(item.getAreaId())).collect(Collectors.groupingBy(TimeCostRes::getAreaId));


                        for (ReturnTypeRes id : nameAndIds) {
                            if(collect.get(id.getId())!= null){
                                List<TimeCostRes> value = collect.get(id.getId());
                                BigDecimal reduce = value.stream().map(TimeCostRes::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
                                TimeCostRes timeCostRes1 = new TimeCostRes();
                                timeCostRes1.setAreaName(id.getName());
                                timeCostRes1.setCost(reduce);
                                timeCostRes.add(timeCostRes1);
                            }else {
                                TimeCostRes timeCostRes1 = new TimeCostRes();
                                timeCostRes1.setAreaName(id.getName());
                                timeCostRes1.setCost(BigDecimal.ZERO);
                                timeCostRes.add(timeCostRes1);
                            }
                        }
                        resultMap.put(key,timeCostRes);
                        return Mono.just(resultMap);
                    }
                });
            }).then(Mono.just(resultMap));
    }

    @Override
    public Mono<Map<String, Object>> getTestYearAnalysis(QueryParamEntity queryParam) {
        String year=queryParam.getContext().get("year").toString();
        String s = JSONArray.toJSONString(queryParam.getContext().get("nameAndIds"));
        List<ReturnTypeRes> nameAndIds= JSON.parseArray(s, ReturnTypeRes.class);
        HashMap<String, Object> resultMap = new HashMap<>();

        List<String> ids = nameAndIds.stream().map(ReturnTypeRes::getId).collect(Collectors.toList());
        Date beginTime = this.getBeginTime(Integer.parseInt(year), 1);
        Date endTime = this.getEndTime(Integer.parseInt(year), 12);

        return testConfigService.createQuery()
            .in(TestConfigEntity::getId,ids)
            .fetch()
            .flatMap(config->{
                return testRecordService.createQuery()
                    .in(TestRecordEntity::getConfigId,config.getId())
                    .fetch()
                    .flatMap(record -> {
                        return testConfigDeviceService
                            .createQuery()
                            .where(TestConfigDeviceEntity::getConfigId, record.getConfigId())
                            .fetch()
                            .map(TestConfigDeviceEntity::getDeviceId)
                            .collectList()
                            .flatMapMany(list -> {
                                if(list.isEmpty()){
                                    return Mono.empty();
                                }
                                //先计算两段时间重叠的部分
                                HashMap<String, DateTime> map = testConfigDeviceService.setOverlap(new DateTime(beginTime.getTime()), new DateTime(endTime.getTime()), new DateTime(record.getTestStartTime()), new DateTime(record.getTestEndTime()));
                                //再求能耗
                                return queryHelper
                                    .select("select t.* from sems_water_consume t  ", TimeCostRes::new)
                                    .where(dsl -> {dsl.in(WaterConsumeEntity::getDeviceId, list);
                                        dsl.between(WaterConsumeEntity::getGatherTime, map.get("endDate").toDate().getTime(), map.get("startDate").toDate().getTime());})
                                    .fetch()
                                    .doOnNext(value->value.setCost(value.getDifference().multiply(value.getUnitPrice())))
                                    .doOnNext(value->value.setTestName(record.getName()))
                                    .doOnNext(value->value.setConfigId(record.getId()));

                            });
                    });
            })
       .collectList()
            .flatMapMany(i->{

                Map<String, List<TimeCostRes>> groupByTime = i.stream().filter(item->StringUtil.isNotBlank(item.getGatherTime())).collect(Collectors.groupingBy(TimeCostRes::getGatherTime));

                return Flux.range(1,12)
                    .flatMap(month->{
                        String key=null;
                        if(month<10){
                            key= year+"-0"+month;
                        }else {
                            key= year+"-"+month;
                        }
                        ArrayList<TimeCostRes> timeCostRes = new ArrayList<>();
                        if(groupByTime.get(key) == null){
                            for (ReturnTypeRes nameAndId : nameAndIds) {
                                TimeCostRes timeCostRes1 = new TimeCostRes();
                                timeCostRes1.setTestName(nameAndId.getName());
                                timeCostRes1.setCost(BigDecimal.ZERO);
                                timeCostRes.add(timeCostRes1);
                            }
                            resultMap.put(key,timeCostRes);
                            return Mono.just(resultMap);

                        }else {
                            List<TimeCostRes> energyCountRes = groupByTime.get(key);
                            Map<String, List<TimeCostRes>> collect = energyCountRes.stream().filter(item-> StringUtil.isNotBlank(item.getConfigId())).collect(Collectors.groupingBy(TimeCostRes::getConfigId));

                            for (ReturnTypeRes id : nameAndIds) {
                                if(collect.get(id.getId())!= null){
                                    List<TimeCostRes> value = collect.get(id.getId());
                                    BigDecimal reduce = value.stream().map(TimeCostRes::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
                                    TimeCostRes timeCostRes1 = new TimeCostRes();
                                    timeCostRes1.setTestName(id.getName());
                                    timeCostRes1.setCost(reduce);
                                    timeCostRes.add(timeCostRes1);
                                }else {
                                    TimeCostRes timeCostRes1 = new TimeCostRes();
                                    timeCostRes1.setTestName(id.getName());
                                    timeCostRes1.setCost(BigDecimal.ZERO);
                                    timeCostRes.add(timeCostRes1);
                                }
                            }
                            resultMap.put(key,timeCostRes);
                            return Mono.just(resultMap);
                        }
                    });
            }).then(Mono.just(resultMap));
    }

    @Override
    public Flux<CostRes> getTestElectricCost(QueryParamEntity query) {
        return null;
    }

    @Override
    public Flux<CostRes> getRegionElectricCost(QueryParamEntity query) {
        return null;
    }

    @Override
    public Flux<CostRes> getTestPeakData(QueryParamEntity query) {
        String startDate = query.getContext().get("startDate").toString();
        String endDate = query.getContext().get("endDate").toString();
        return queryHelper.select("SELECT * FROM sems_test_record WHERE not (test_start_time > " + endDate + " or test_end_time < " + startDate + ")", TestRecordEntity::new)
            .where(new QueryParamEntity())
            .fetch()
            .flatMap(record -> {
                return testConfigDeviceService
                    .createQuery()
                    .where(TestConfigDeviceEntity::getConfigId, record.getConfigId())
                    .fetch()
                    .map(TestConfigDeviceEntity::getDeviceId)
                    .collectList()
                    .flatMapMany(list -> {
                        if(list.isEmpty()){
                            return Mono.empty();
                        }
                        //先计算两段时间重叠的部分
                        HashMap<String, DateTime> map = testConfigDeviceService.setOverlap(new DateTime(Long.parseLong(startDate)), new DateTime(Long.parseLong(endDate)), new DateTime(record.getTestStartTime()), new DateTime(record.getTestEndTime()));
                        //再求能耗
                        return queryHelper
                            .select("select t.* from sems_water_consume t  ", TimeCostRes::new)
                            .where(dsl -> {dsl.in(WaterConsumeEntity::getDeviceId, list);
                                dsl.between(WaterConsumeEntity::getGatherTime, map.get("startDate").toDate().getTime(), map.get("endDate").toDate().getTime());
                            })
                            .fetch()
                            .doOnNext(value->value.setCost(value.getDifference().multiply(value.getUnitPrice())))
                            .doOnNext(value->value.setTestName(record.getName()))
                            .doOnNext(value->value.setConfigId(record.getId()));

                    });
            }).collectList()
            .flatMapMany(list->{
                Map<String, BigDecimal> map = list.stream().collect(Collectors.toMap(TimeCostRes::getTestName, TimeCostRes::getCost, BigDecimal::add));
                ArrayList<CostRes> costRes = new ArrayList<>();
                for (Map.Entry<String, BigDecimal> stringBigDecimalEntry : map.entrySet()) {
                    String key = stringBigDecimalEntry.getKey();
                    BigDecimal value = stringBigDecimalEntry.getValue();
                    costRes.add(CostRes.builder().testName(key).cost(value).build());
                }
                List<CostRes> resultList= costRes.stream().sorted(Comparator.comparing(CostRes::getTestName)).collect(Collectors.toList());
                return Flux.fromIterable(resultList);
            });
    }

    private Mono<PeakAnalysisRes> getPeakAnalysisData(QueryParamEntity query,String type) {
        query.setPaging(false);
//        return waterConsumeService
//            .query(query)
        String startDate = query.getContext().get("startDate").toString();
        String endDate = query.getContext().get("endDate").toString();
        return peakAnalysis(startDate,endDate)
            .collectMultimap(chkTimeFunction(type,Long.valueOf(endDate)))
            .map(WaterStrategy::getReduce)
            .map(PeakAnalysisRes::new)
            .map(peakAnalysisRes -> TimeHandler.generateTimes(query,type,peakAnalysisRes));
    }

    private static Function<WaterConsumeEntity, Long> chkTimeFunction(String type,Long endDate) {
        return entity -> {
            if (type.equals(TimeEnum.DAY.name())){
                return   TimeHandler.dateChangeByDayAndTimeStep(entity.getGatherTime(),endDate);
            }else if(type.equals(TimeEnum.YEAR.name())){
                return TimeHandler.dateChangeByYearAndTimeStep(entity.getGatherTime());
            }
            return TimeHandler.dateChangeByMonthAndTimeStep(entity.getGatherTime());

        };
    }

    private static HashMap<Long, BigDecimal> getReduce(Map<Long, Collection<WaterConsumeEntity>> entityMap) {
        return entityMap
            .entrySet()
            .stream()
            .collect(
                HashMap::new,
                (map, entry) -> map.put(entry.getKey(), entry.getValue().stream()
                                                                .filter(l->l.getDifference()!= null)
                                                                  .map(WaterConsumeEntity::getDifference)
                                                                  .max(BigDecimal::compareTo)
                                                                  .orElse(BigDecimal.ZERO)),
                HashMap::putAll
            );
    }

    public Mono<List<TimeCostRes>> getEnergyByAreaId(String areaId,String areaName, Integer energyType, Long startDate, Long endDate) {
        ArrayList<TimeCostRes> timeCostRes = new ArrayList<>();
        return areaInfoService.getRegionTree(areaId, energyType)
            .flatMap(value -> {
                return Flux.fromIterable(value)
                    .flatMap(areaInfoEntity -> {
                        return deviceService.getDeviceIdByAreaId(areaInfoEntity.getId(), energyType)
                            .filter(va -> !va.isEmpty())
                            .flatMap(deviceIds -> {
                                return queryHelper
                                    .select("select DATE_FORMAT(FROM_UNIXTIME(substr(t.gather_time,1,10),'%Y-%m-%d %H:%i:%S'),'%Y-%m') as gatherTime, t.difference as difference, t.unit_price as unitPrice from sems_water_consume t ", TimeCostRes::new)
                                    .where(dsl -> {
                                        dsl.in(WaterConsumeEntity::getDeviceId, deviceIds);
                                            dsl.between(WaterConsumeEntity::getGatherTime, startDate, endDate);
                                    })
                                    .fetch()
                                    .doOnNext(i ->
                                        i.setCost(i.getDifference() == null ? BigDecimal.ZERO : i.getDifference().multiply(i.getUnitPrice() == null ? BigDecimal.ZERO : i.getUnitPrice()))
                                    )
                                    .doOnNext(i -> i.setAreaName(areaName))
                                    .doOnNext(i -> i.setAreaId(areaId))
                                    .cache()
                                    .collectList()
                                    .flatMap(list -> {
                                        timeCostRes.addAll(list);
                                        return Mono.just(timeCostRes);
                                    });
                            });
                    }).then();
            }).thenReturn(timeCostRes);
    }

        //自定义分析
        public Mono<CostRes> getEnergyByAreaIdSelf(String areaId,String areaName, Integer energyType, Long startDate, Long endDate) {
            return areaInfoService.getRegionTree(areaId, energyType)
                .flatMap(value -> {
                    return Flux.fromIterable(value)
                        .flatMap(areaInfoEntity -> {
                            return deviceService.getDeviceIdByAreaId(areaInfoEntity.getId(), energyType)
                                .filter(va -> !va.isEmpty())
                                .flatMap(deviceIds -> {
                                    return queryHelper
                                        .select("select t.difference as difference, t.unit_price as unitPrice from sems_water_consume t ", TimeCostRes::new)
                                        .where(dsl -> {
                                            dsl.in(WaterConsumeEntity::getDeviceId, deviceIds);
                                            dsl.between(WaterConsumeEntity::getGatherTime, startDate, endDate);})
                                        .fetch()
                                        .doOnNext(i ->
                                            i.setCost(i.getDifference() == null ? BigDecimal.ZERO : i.getDifference().multiply(i.getUnitPrice() == null ? BigDecimal.ZERO : i.getUnitPrice()))
                                        )
                                        .map(TimeCostRes::getCost)
                                        .reduce(BigDecimal.ZERO,BigDecimal::add);
                                });
                        }).reduce(BigDecimal.ZERO,BigDecimal::add)
                        .flatMap(num->{
                            CostRes build = CostRes.builder()
                                .cost(num)
                                .region(areaName)
                                .build();
                            return Mono.just(build);
                        });
                });
        };

    public static Date getBeginTime(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate localDate = yearMonth.atDay(1);
        LocalDateTime startOfDay = localDate.atStartOfDay();
        ZonedDateTime zonedDateTime = startOfDay.atZone(ZoneId.of("Asia/Shanghai"));

        return Date.from(zonedDateTime.toInstant());
    }

    public static Date getEndTime(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();
        LocalDateTime localDateTime = endOfMonth.atTime(23, 59, 59, 999);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of("Asia/Shanghai"));
        return Date.from(zonedDateTime.toInstant());
    }

    public Flux<WaterConsumeEntity> peakAnalysis(String startDate, String endDate){
        return areaInfoService
            .createQuery()
            .where(AreaInfoEntity::getParentId, "0")
            .where(AreaInfoEntity::getState,"0")
            .fetch()
            .flatMap(areaInfo -> {
                return areaInfoService.getDeviceIds(areaInfo.getId(),"water")
                    .collectList()
                    .flatMapMany(devices->{
                        if(devices.isEmpty()){
                            return Flux.empty();
                        }
                        return waterConsumeService
                            .createQuery()
                            .in(ElectricityConsumeEntity::getDeviceId,devices)
                            .gte(ElectricityConsumeEntity::getGatherTime,startDate)
                            .lte(ElectricityConsumeEntity::getGatherTime,endDate)
                            .fetch();
                    });

            });

    }
}
