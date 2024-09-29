package org.jetlinks.pro.sems.strategy.peak.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import lombok.RequiredArgsConstructor;
import org.apache.poi.util.StringUtil;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.sems.entity.*;
import org.jetlinks.pro.sems.entity.res.*;
import org.jetlinks.pro.sems.service.*;
import org.jetlinks.pro.sems.strategy.peak.PeakAnalysisStrategy;
import org.jetlinks.pro.sems.strategy.peak.common.TimeHandler;
import org.jetlinks.pro.sems.enums.TimeEnum;
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
 * @ClassName ElectricityStrategy
 * @Author hky
 * @Time 2023/7/12 17:30
 * @Description 电
 **/
@Component("electricity")
@RequiredArgsConstructor
public class ElectricityStrategy implements PeakAnalysisStrategy {

    private final ElectricityConsumeService electricityConsumeService;
    private final QueryHelper queryHelper;

    private final AreaInfoService areaInfoService;

    private final DeviceService deviceService;

    private final TestConfigDeviceService testConfigDeviceService;

    private final TestRecordService testRecordService;

    private final TestConfigService testConfigService;

    @Override
    public Mono<PeakAnalysisRes> getPeakDetail(QueryParamEntity query, TimeEnum timeEnum) {
        return getPeakAnalysisData(query, timeEnum.name());
    }

    @Override
    public Flux<PeakUniversalData> getPeakData(QueryParamEntity query) {
        query.setPaging(false);
        return electricityConsumeService
            .query(query)
            .filter(i -> i.getDifference() != null && i.getCostId() != null)
            .map(PeakUniversalData::new);

    }

    @Override
    public Mono<List<CostRes>> getRegionPeakData(QueryParamEntity query) {
        return queryHelper
            .select(CostResExtendRes.class)
            .as(ElectricityConsumeEntity::getDifference,CostResExtendRes::setDifference)
            .as(ElectricityConsumeEntity::getPeriodsType,CostResExtendRes::setPeriodsType)
            .as(ElectricityConsumeEntity::getUnitPrice,CostResExtendRes::setUnitPrice)
            .as(DeviceInfoEntity::getDeviceName,CostResExtendRes::setDeviceName)
            .from(ElectricityConsumeEntity.class)
            .leftJoin(DeviceInfoEntity.class,spec->spec.is(DeviceInfoEntity::getDeviceId,ElectricityConsumeEntity::getDeviceId))
            .where(query)
            .fetch()
            .filter(value->value.getUnitPrice()!=null)
            .doOnNext(value->value.setUnitPrice(value.getUnitPrice().multiply(value.getDifference())))
            .collectList()
            .flatMap(list->{
                ArrayList<CostRes> costResList = new ArrayList<>();
                Map<String, List<CostResExtendRes>> collect = list.stream().collect(Collectors.groupingBy(CostResExtendRes::getDeviceName));
                for (Map.Entry<String, List<CostResExtendRes>> stringListEntry : collect.entrySet()) {
                    List<CostResExtendRes> value = stringListEntry.getValue();
                    BigDecimal bigDecimalNu = value.stream().map(CostResExtendRes::getUnitPrice).reduce(BigDecimal.ZERO,BigDecimal::add);
                    CostResExtendRes costRes = new CostResExtendRes();
                    costRes.setDeviceName(stringListEntry.getKey());
                    costRes.setUnitPrice(bigDecimalNu.setScale(2,BigDecimal.ROUND_HALF_UP));
                    costResList.add(CostRes.builder().region(stringListEntry.getKey()).cost(bigDecimalNu).build());

                }
                return Mono.just(costResList);
            });
    }

    @Override
    public Mono<Map<String, Object>> getRegionYearAnalysis(QueryParamEntity queryParam) {
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
                return this.getEnergyByAreaId(nameAndId.getId(),nameAndId.getName(),2,startOfDay.toInstant(ZoneOffset.of("+8")).toEpochMilli(),endOfDay.toInstant(ZoneOffset.of("+8")).toEpochMilli())
                    .flatMap(list->{
                        timeCostRes2.addAll(list);
                        return Mono.just(timeCostRes2);
                    });
            })
            .then(Mono.just(timeCostRes2))

            .flatMapMany(i -> {

                Map<String, List<TimeCostRes>> groupByTime = i.stream().collect(Collectors.groupingBy(TimeCostRes::getGatherTime));

                return Flux.range(1, 12)
                    .flatMap(month -> {
                        String key = null;
                        if (month < 10) {
                            key = year + "-0" + month;
                        } else {
                            key = year + "-" + month;
                        }

                        ArrayList<TimeCostRes> timeCostRes = new ArrayList<>();
                        if (groupByTime.get(key) == null) {
                            for (ReturnTypeRes nameAndId : nameAndIds) {
                                TimeCostRes timeCostRes1 = new TimeCostRes();
                                timeCostRes1.setAreaName(nameAndId.getName());
                                timeCostRes1.setCost(BigDecimal.ZERO);
                                timeCostRes.add(timeCostRes1);
                            }
                            resultMap.put(key, timeCostRes);
                            return Mono.just(resultMap);

                        } else {
                            List<TimeCostRes> energyCountRes = groupByTime.get(key);
                            Map<String, List<TimeCostRes>> collect = energyCountRes.stream().filter(item -> StringUtil.isNotBlank(item.getGatherTime())).collect(Collectors.groupingBy(TimeCostRes::getAreaId));

                            for (ReturnTypeRes nameAndId : nameAndIds) {
                                if (collect.get(nameAndId.getId()) != null) {
                                    BigDecimal reduce = collect.get(nameAndId.getId()).stream().map(TimeCostRes::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
                                    TimeCostRes timeCostRes1 = new TimeCostRes();
                                    timeCostRes1.setAreaName(nameAndId.getName());
                                    timeCostRes1.setCost(reduce);
                                    timeCostRes.add(timeCostRes1);
                                } else {
                                    TimeCostRes timeCostRes1 = new TimeCostRes();
                                    timeCostRes1.setAreaName(nameAndId.getName());
                                    timeCostRes1.setCost(BigDecimal.ZERO);
                                    timeCostRes.add(timeCostRes1);
                                }
                            }
                            resultMap.put(key, timeCostRes);
                            return Mono.just(resultMap);
                        }
                    });
            }).then(Mono.just(resultMap));
    }

    @Override
    public Mono<Map<String, Object>> getTestYearAnalysis(QueryParamEntity queryParam) {
        String year = queryParam.getContext().get("year").toString();
        String s = JSONArray.toJSONString(queryParam.getContext().get("nameAndIds"));
        List<ReturnTypeRes> nameAndIds = JSON.parseArray(s, ReturnTypeRes.class);
        List<String> ids = nameAndIds.stream().map(ReturnTypeRes::getId).collect(Collectors.toList());

        HashMap<String, Object> resultMap = new HashMap<>();


        Date beginTime = this.getBeginTime(Integer.parseInt(year), 1);
        Date endTime = this.getEndTime(Integer.parseInt(year), 12);
        return testConfigService.createQuery()
            .in(TestConfigEntity::getId,ids)
            .fetch()
            .flatMap(config->{
                return testRecordService.createQuery()
                    .where(TestRecordEntity::getConfigId,config.getId())
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
                                    .select("select DATE_FORMAT(FROM_UNIXTIME(substr(t.gather_time,1,10),'%Y-%m-%d %H:%i:%S'),'%Y-%m') as gatherTime,t.difference,t.unit_price as unitPrice from sems_electricity_consume t  ", TimeCostRes::new)
                                    .where(dsl -> {
                                        dsl.in(ElectricityConsumeEntity::getDeviceId, list);
                                        dsl.between(ElectricityConsumeEntity::getGatherTime, map.get("startDate").toDate().getTime(), map.get("endDate").toDate().getTime());
                                    })
                                    .fetch()
                                    .doOnNext(value->value.setCost(value.getDifference().multiply(value.getUnitPrice())))
                                    .doOnNext(value->value.setTestName(config.getTestName()))
                                    .doOnNext(value->value.setConfigId(config.getId()));
                            });
                    });
            }).collectList()
            .flatMapMany(i -> {
                Map<String, List<TimeCostRes>> groupByTime = i.stream().filter(item -> StringUtil.isNotBlank(item.getGatherTime())).collect(Collectors.groupingBy(TimeCostRes::getGatherTime));

                return Flux.range(1, 12)
                    .flatMap(month -> {
                        String key = null;
                        if (month < 10) {
                            key = year + "-0" + month;
                        } else {
                            key = year + "-" + month;
                        }
                        ArrayList<TimeCostRes> timeCostRes = new ArrayList<>();
                        if (groupByTime.get(key) == null) {
                            for (ReturnTypeRes nameAndId : nameAndIds) {
                                TimeCostRes timeCostRes1 = new TimeCostRes();
                                timeCostRes1.setTestName(nameAndId.getName());
                                timeCostRes1.setCost(BigDecimal.ZERO);
                                timeCostRes.add(timeCostRes1);
                            }
                            resultMap.put(key, timeCostRes);
                            return Mono.just(resultMap);

                        } else {
                            List<TimeCostRes> energyCountRes = groupByTime.get(key);
                            Map<String, List<TimeCostRes>> collect = energyCountRes.stream().filter(item -> StringUtil.isNotBlank(item.getConfigId())).collect(Collectors.groupingBy(TimeCostRes::getConfigId));

                            for (ReturnTypeRes nameAndId : nameAndIds) {
                                if (collect.get(nameAndId.getId()) != null) {
                                    BigDecimal reduce = collect.get(nameAndId.getId()).stream().map(TimeCostRes::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
                                    TimeCostRes timeCostRes1 = new TimeCostRes();
                                    timeCostRes1.setTestName(nameAndId.getName());
                                    timeCostRes1.setCost(reduce);
                                    timeCostRes.add(timeCostRes1);
                                } else {
                                    TimeCostRes timeCostRes1 = new TimeCostRes();
                                    timeCostRes1.setTestName(nameAndId.getName());
                                    timeCostRes1.setCost(BigDecimal.ZERO);
                                    timeCostRes.add(timeCostRes1);
                                }
                            }
                            resultMap.put(key, timeCostRes);
                            return Mono.just(resultMap);
                        }
                    });
            }).then(Mono.just(resultMap));
    }

    @Override
    public Flux<CostRes> getTestElectricCost(QueryParamEntity query) {
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
                            .select("select t.*,t.periods_type as periodsType from sems_electricity_consume t  ", TimeCostRes::new)
                            .where(dsl -> dsl.in("device_id", list).between("gather_time", map.get("startDate").toDate().getTime(), map.get("endDate").toDate().getTime()))
                            .fetch()
                            .doOnNext(value->value.setCost(value.getDifference().multiply(value.getUnitPrice())))
                            .doOnNext(value->value.setTestName(record.getName()))
                            .doOnNext(value->value.setConfigId(record.getId()));

                    });
            }).collectList()
            .flatMapMany(i -> {
                ArrayList<CostRes> costRes = new ArrayList<>();
                Map<String, List<TimeCostRes>> collect = i.stream().filter(item -> StringUtil.isNotBlank(item.getConfigId())).collect(Collectors.groupingBy(TimeCostRes::getConfigId));
                Map<String, String> testMap = i.stream().collect(Collectors.toMap(TimeCostRes::getConfigId, TimeCostRes::getTestName, (c1, c2) -> c1));
                for (Map.Entry<String, List<TimeCostRes>> stringListEntry : collect.entrySet()) {
                    String configId = stringListEntry.getKey();
                    List<TimeCostRes> value = stringListEntry.getValue();
                    CostRes.CostResBuilder test = CostRes.builder().region(testMap.get(configId));
                    Map<String, List<TimeCostRes>> colletMap = value.stream().filter(item -> StringUtil.isNotBlank(item.getPeriodsType())).collect(Collectors.groupingBy(TimeCostRes::getPeriodsType));
                    for (Map.Entry<String, List<TimeCostRes>> listEntry : colletMap.entrySet()) {
                        String key = listEntry.getKey();
                        List<TimeCostRes> costList = listEntry.getValue();
                        BigDecimal reduce = costList.stream().map(TimeCostRes::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
                        switch (key) {
                            case "1":
                                test.cuspPeriods(reduce);
                                break;
                            case "2":
                                test.peakPeriods(reduce);
                                break;
                            case "3":
                                test.flatPeriods(reduce);
                                break;
                            case "4":
                                test.valleyPeriods(reduce);
                                break;
                        }
                    }
                    costRes.add(test.build());
                }
                List<CostRes> resultList= costRes.stream().sorted(Comparator.comparing(CostRes::getTestName)).collect(Collectors.toList());
                return Flux.fromIterable(resultList);
            });
    }

    @Override
    public Flux<CostRes> getRegionElectricCost(QueryParamEntity query) {

        return queryHelper
            .select(CostResExtendRes.class)
            .as(ElectricityConsumeEntity::getDifference,CostResExtendRes::setDifference)
            .as(ElectricityConsumeEntity::getPeriodsType,CostResExtendRes::setPeriodsType)
            .as(ElectricityConsumeEntity::getUnitPrice,CostResExtendRes::setUnitPrice)
            .as(DeviceInfoEntity::getDeviceName,CostResExtendRes::setDeviceName)
            .from(ElectricityConsumeEntity.class)
            .leftJoin(DeviceInfoEntity.class,spec->spec.is(DeviceInfoEntity::getDeviceId,ElectricityConsumeEntity::getDeviceId))
            .where(query)
            .fetch()
            .doOnNext(value->{
                if(value.getPeriodsType()!= null){
                    if(value.getPeriodsType()==1){
                        //尖峰
                        value.setCuspPeriods(value.getDifference().multiply(value.getUnitPrice()).setScale(2,BigDecimal.ROUND_HALF_UP));
                    }else if(value.getPeriodsType()==2){
                        value.setPeakPeriods(value.getDifference().multiply(value.getUnitPrice()).setScale(2,BigDecimal.ROUND_HALF_UP));
                    }else if(value.getPeriodsType()==3){
                        value.setFlatPeriods(value.getDifference().multiply(value.getUnitPrice()).setScale(2,BigDecimal.ROUND_HALF_UP));
                    }else if(value.getPeriodsType()==4){
                        value.setValleyPeriods(value.getDifference().multiply(value.getUnitPrice()).setScale(2,BigDecimal.ROUND_HALF_UP));
                    }
                }

            }).collectList()
            .flatMapMany(value->{
                ArrayList<CostRes> costRes1 = new ArrayList<>();
                Map<String, List<CostResExtendRes>> collect = value.stream().collect(Collectors.groupingBy(CostResExtendRes::getDeviceName));
                for (Map.Entry<String, List<CostResExtendRes>> stringListEntry : collect.entrySet()) {
                    List<CostResExtendRes> value1 = stringListEntry.getValue();

                    BigDecimal p1=BigDecimal.ZERO;
                    BigDecimal p2=BigDecimal.ZERO;
                    BigDecimal p3=BigDecimal.ZERO;
                    BigDecimal p4=BigDecimal.ZERO;
                    for (CostResExtendRes costRes : value1) {
                        p1=p1.add(costRes.getCuspPeriods()==null?BigDecimal.ZERO:costRes.getCuspPeriods());
                        p2=p2.add(costRes.getPeakPeriods()==null?BigDecimal.ZERO:costRes.getPeakPeriods());
                        p3=p3.add(costRes.getFlatPeriods()==null?BigDecimal.ZERO:costRes.getFlatPeriods());
                        p4=p4.add(costRes.getValleyPeriods()==null?BigDecimal.ZERO:costRes.getValleyPeriods());
                    }
                    costRes1.add(CostRes.builder().region(stringListEntry.getKey())
                        .cuspPeriods(p1).peakPeriods(p2).flatPeriods(p3).valleyPeriods(p4)
                        .build());
                }
                return Flux.fromIterable(costRes1);
            });


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
                            .select("select t.* from sems_electricity_consume t  ", TimeCostRes::new)
                            .where(dsl -> {
                                dsl.in(ElectricityConsumeEntity::getDeviceId, list);
                                dsl.nest().between(ElectricityConsumeEntity::getGatherTime, map.get("startDate").toDate().getTime(), map.get("endDate").toDate().getTime()).end();
                            })
                            .fetch()
                            .doOnNext(value->value.setCost(value.getDifference().multiply(value.getUnitPrice())))
                            .doOnNext(value->value.setTestName(record.getTestName()))
                            .doOnNext(value->value.setConfigId(record.getConfigId()));

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


    private Mono<PeakAnalysisRes> getPeakAnalysisData(QueryParamEntity query, String type) {
        query.setPaging(false);
//        return electricityConsumeService
//            .query(query)
        String startDate = query.getContext().get("startDate").toString();
        String endDate = query.getContext().get("endDate").toString();
        return this.peakAnalysis(startDate,endDate)
            .filter(y -> y.getDifference() != null)
            .collectMultimap(chkTimeFunction(type,Long.valueOf(endDate)))
            .map(ElectricityStrategy::getReduce)
            .map(PeakAnalysisRes::new)
            .map(peakAnalysisRes -> TimeHandler.generateTimes(query, type, peakAnalysisRes));
    }

    private static Function<ElectricityConsumeEntity, Long> chkTimeFunction(String type,Long endDate) {
        return entity -> {
            if (type.equals(TimeEnum.DAY.name())) {
                return TimeHandler.dateChangeByDayAndTimeStep(entity.getGatherTime(),endDate);
            } else if (type.equals(TimeEnum.YEAR.name())) {
                return TimeHandler.dateChangeByYearAndTimeStep(entity.getGatherTime());
            }
            return TimeHandler.dateChangeByMonthAndTimeStep(entity.getGatherTime());

        };
    }

    private static HashMap<Long, BigDecimal> getReduce(Map<Long, Collection<ElectricityConsumeEntity>> entityMap) {
        return entityMap
            .entrySet()
            .stream()
            .collect(
                HashMap::new,
                (map, entry) -> map.put(entry.getKey(), entry.getValue().stream()
                    .map(ElectricityConsumeEntity::getDifference)
                    .reduce(BigDecimal::add)
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
                                    .select("select DATE_FORMAT(FROM_UNIXTIME(substr(t.gather_time,1,10),'%Y-%m-%d %H:%i:%S'),'%Y-%m') as gatherTime, t.difference as difference, t.unit_price as unitPrice from sems_electricity_consume t ", TimeCostRes::new)

                                    .where(dsl -> {
                                        dsl.in(ElectricityConsumeEntity::getDeviceId, deviceIds);
                                            dsl.between(ElectricityConsumeEntity::getGatherTime,startDate,endDate);
                                    })
                                    .fetch()
                                    .doOnNext(i ->
                                        i.setCost(i.getDifference() == null ? BigDecimal.ZERO : i.getDifference().multiply(i.getUnitPrice() == null ? BigDecimal.ZERO : i.getUnitPrice()))
                                    )
                                    .doOnNext(i->i.setAreaName(areaName))
                                    .doOnNext(i->i.setAreaId(areaId))
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
                                return electricityConsumeService.createQuery()
                                    .in(ElectricityConsumeEntity::getDeviceId,deviceIds)
                                    .between(ElectricityConsumeEntity::getGatherTime,startDate,endDate)
                                    .fetch()
                                    .map(info-> FastBeanCopier.copy(info,TimeCostRes::new))
//                                return queryHelper
//                                    .select("select t.difference as difference, t.unit_price as unitPrice from sems_electricity_consume t ", TimeCostRes::new)
//                                    .where(dsl -> dsl.in("device_id", deviceIds).between("gather_time", startDate, endDate))
//                                    .fetch()
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

    //自定义分析(成本条目)
    public Mono<List<TimeCostRes>> getEnergyByAreaIdAndType(String areaId,String areaName, Integer energyType, Long startDate, Long endDate) {
        ArrayList<TimeCostRes> timeCostRes = new ArrayList<>();
        TimeCostRes timeCostRes1 = new TimeCostRes();
        return areaInfoService.getRegionTree(areaId, energyType)
            .flatMap(value -> {
                return Flux.fromIterable(value)
                    .flatMap(areaInfoEntity -> {
                        return deviceService.getDeviceIdByAreaId(areaInfoEntity.getId(), energyType)
                            .filter(va -> !va.isEmpty())
                            .flatMap(deviceIds -> {
                                return electricityConsumeService.createQuery()
                                    .in(ElectricityConsumeEntity::getDeviceId,deviceIds)
                                    .between(ElectricityConsumeEntity::getGatherTime,startDate,endDate)
                                    .fetch()
                                    .map(info-> FastBeanCopier.copy(info,TimeCostRes::new))
//                                return queryHelper
//                                    .select("select t.difference as difference, t.unit_price as unitPrice ,t.periods_type as periodsType  from sems_electricity_consume t ", TimeCostRes::new)
//                                    .where(dsl -> dsl.in("device_id", deviceIds).between("gather_time", startDate, endDate))
//                                    .fetch()
                                    .defaultIfEmpty(timeCostRes1)
                                    .doOnNext(i -> i.setAreaName(areaName))
                                    .doOnNext(i -> i.setAreaId(areaId))
                                    .doOnNext(i ->
                                        i.setCost(i.getDifference() == null ? BigDecimal.ZERO : i.getDifference().multiply(i.getUnitPrice() == null ? BigDecimal.ZERO : i.getUnitPrice()))
                                    )
                                    .collectList()
                                    .flatMap(list -> {
                                        timeCostRes.addAll(list);
                                        return Mono.just(timeCostRes);
                                    });

                            });
                    }).then();
            }).thenReturn(timeCostRes);
    }

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


    public Flux<ElectricityConsumeEntity> peakAnalysis(String startDate,String endDate){
        return areaInfoService
            .createQuery()
            .where(AreaInfoEntity::getParentId, "0")
            .where(AreaInfoEntity::getState,"0")
            .fetch()
            .flatMap(areaInfo -> {
                return areaInfoService.getDeviceIds(areaInfo.getId(),"electricity")
                    .collectList()
                    .flatMapMany(devices->{
                        if(devices.isEmpty()){
                            return Flux.empty();
                        }
                        return electricityConsumeService
                            .createQuery()
                            .in(ElectricityConsumeEntity::getDeviceId,devices)
                            .gte(ElectricityConsumeEntity::getGatherTime,startDate)
                            .lte(ElectricityConsumeEntity::getGatherTime,endDate)
                            .fetch();
                    });

            });

    }

}
