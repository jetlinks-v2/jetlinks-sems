package org.jetlinks.pro.sems.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.sems.entity.req.AnalysisCostReq;
import org.jetlinks.pro.sems.entity.res.CostAnalysisReport;
import org.jetlinks.pro.sems.entity.res.CostReturnRes;
import org.jetlinks.pro.sems.entity.res.EntryMonthRes;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.jetlinks.pro.sems.entity.CostConfigEntity;
import org.jetlinks.pro.sems.entity.ElectricityIntervalEntity;
import org.jetlinks.pro.sems.entity.TestAreaEntity;
import org.jetlinks.pro.sems.factory.CastFactory;
import org.jetlinks.pro.sems.service.CostConfService;
import org.jetlinks.pro.sems.service.ElectricityConsumeService;
import org.jetlinks.pro.sems.service.ElectricityIntervalService;
import org.jetlinks.pro.sems.utils.DateUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sems/costing")
@AllArgsConstructor
@Getter
@Tag(name = "成本分析1.0")
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "costing", name = "成本分析")
public class CostingController {

    private final CastFactory castFactory;

    private final QueryHelper queryHelper;

    private final ElectricityConsumeService electricityConsumeService;

    private final ElectricityIntervalService electricityIntervalService;

    private final CostConfService costConfService;


    /**
     * 成本条目按年返回方法
     * @param year
     * @return
     */
    public synchronized Flux<EntryMonthRes> yearEntryAnalysis(String year, List<String> areaNames){
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.YEAR,Integer.valueOf(year));
        //开始时间

        instance.set(Calendar.DAY_OF_YEAR,instance.getActualMinimum(Calendar.DAY_OF_YEAR));
        Date startTime = instance.getTime();
        String start=DateUtil.dateToString(startTime,DateUtil.DATE_SHORT_FORMAT)+ " 00:00:00";
        Date dateS = DateUtil.stringToDate(start, DateUtil.DATE_WITHSECOND_FORMAT);
        //结束时间
        instance.set(Calendar.DAY_OF_YEAR,instance.getActualMaximum(Calendar.DAY_OF_YEAR));
        Date endTime = instance.getTime();
        String end=DateUtil.dateToString(endTime,DateUtil.DATE_SHORT_FORMAT)+ " 23:59:59";
        Date dateE = DateUtil.stringToDate(end, DateUtil.DATE_WITHSECOND_FORMAT);
        Long[] longs={dateS.getTime(),dateE.getTime()};

        return queryHelper
            .select("SELECT DATE_FORMAT( FROM_UNIXTIME( substr( gather_time, 1, 10 ), '%Y-%m-%d %H:%i:%S' ), '%m' ) " +
                "as month,t.unit_price*t.difference  as cost, t.periods_type as type ,t.difference as difference,sta.area_name as areaName from sems_electricity_consume t " +
                    "left join sems_device_info sdi on  sdi.device_id=t.device_id  inner join   sems_test_area sta  on sta.id=sdi.place_id  "
                , EntryMonthRes::new)
            .where(dsl->{
                dsl.and("gather_time","btw",longs)
                    .and("sta.id","in",areaNames);
            })
            .fetch()
            .filter(value->value.getCost()!= null)
            .collectList()
            .flatMapMany(list->{
                ArrayList<EntryMonthRes> result = new ArrayList<>();


                Map<String, List<EntryMonthRes>> collect = list.stream().collect(Collectors.groupingBy(EntryMonthRes::getMonth));
                for (int i = 1; i <= 12; i++) {
                    EntryMonthRes entryMonthRes = new EntryMonthRes();
                    HashMap<String, BigDecimal> stringBigDecimalHashMap = new HashMap<>();
                    stringBigDecimalHashMap.put("totalConsumption",BigDecimal.ZERO);
                    stringBigDecimalHashMap.put("EnergyConsumptionCost",BigDecimal.ZERO);
                    entryMonthRes.setCuspPeriods(stringBigDecimalHashMap);
                    entryMonthRes.setFlatPeriods(stringBigDecimalHashMap);
                    entryMonthRes.setPeakPeriods(stringBigDecimalHashMap);
                    entryMonthRes.setValleyPeriods(stringBigDecimalHashMap);
                    String s1=null;
                    if(i<10){
                         s1="0"+i;
                    }else {
                        s1=""+i;
                    }
                    if(collect.get(s1) != null){
                        entryMonthRes.setMonth(s1);
                        List<EntryMonthRes> costList = collect.get(s1);

                        Map<Integer, List<EntryMonthRes>> collect1 = costList.stream().collect(Collectors.groupingBy(EntryMonthRes::getType));
                        for (Map.Entry<Integer, List<EntryMonthRes>> integerListEntry : collect1.entrySet()) {
                            Integer key = integerListEntry.getKey();
                            //对应月份各阶段的总能耗
                            BigDecimal totalEnergy = integerListEntry.getValue().stream().map(EntryMonthRes::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add);
                            BigDecimal reduce = integerListEntry.getValue().stream().map(EntryMonthRes::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
                            if(key==1){
                                HashMap<String, BigDecimal> hashMap = new HashMap<>();
                                hashMap.put("totalConsumption",totalEnergy);
                                hashMap.put("EnergyConsumptionCost",reduce);
                                entryMonthRes.setCuspPeriods(hashMap);
                            }else if(key==2){
                                HashMap<String, BigDecimal> hashMap = new HashMap<>();
                                hashMap.put("totalConsumption",totalEnergy);
                                hashMap.put("EnergyConsumptionCost",reduce);
                                entryMonthRes.setPeakPeriods(hashMap);
                            }else if(key==3){
                                HashMap<String, BigDecimal> hashMap = new HashMap<>();
                                hashMap.put("totalConsumption",totalEnergy);
                                hashMap.put("EnergyConsumptionCost",reduce);
                                entryMonthRes.setFlatPeriods(hashMap);
                            }else if(key==4){
                                HashMap<String, BigDecimal> hashMap = new HashMap<>();
                                hashMap.put("totalConsumption",totalEnergy);
                                hashMap.put("EnergyConsumptionCost",reduce);
                                entryMonthRes.setValleyPeriods(hashMap);
                            }
                        }
                        result.add(entryMonthRes);
                    }
                }
                return Flux.fromIterable(result);
            });
    }


    @Operation(summary = "年度分析成本条目")
    @PostMapping("/yearEntry")
    @QueryAction
    public Flux<EntryMonthRes> yearAnalysis(@RequestBody AnalysisCostReq analysisCostReq){
        List<String> areaNames = analysisCostReq.getTestAreaList().stream().map(TestAreaEntity::getId).collect(Collectors.toList());
        if(analysisCostReq.getDimition()==1){
            String startTime = analysisCostReq.getStartTime();
            String year = startTime.substring(0, 4);
            return this.yearEntryAnalysis(year,areaNames);
        }else {
            //时间转时间戳(开始)
            String startTime = analysisCostReq.getStartTime()+" 00:00:00";
            long timeS = DateUtil.stringToDate(startTime, DateUtil.DATE_WITHSECOND_FORMAT).getTime();
            //时间转时间戳(结束)
            String endTime = analysisCostReq.getEndTime();
            endTime=endTime+" 23:59:59";
            long timeE = DateUtil.stringToDate(endTime, DateUtil.DATE_WITHSECOND_FORMAT).getTime();

            Long[]longs={timeS,timeE};
            List<String> timeList = this.getList(analysisCostReq.getStartTime(), analysisCostReq.getEndTime());

            return queryHelper
                .select("SELECT DATE_FORMAT( FROM_UNIXTIME( substr( sec.gather_time, 1, 10 ), '%Y-%m-%d %H:%i:%S' ), '%Y-%m-%d' ) \n" +
                        "                        as month,sec.unit_price*sec.difference  as cost, sec.periods_type as type " +
                        ",sec.difference as difference from sems_electricity_consume sec left join sems_device_info sdi on " +
                        " sdi.device_id=sec.device_id  inner join   sems_test_area sta  on sta.id=sdi.place_id    "
                    , EntryMonthRes::new)
                .where(dsl->{
                    dsl.and("gather_time","btw",longs).
                    and("sta.id","in",areaNames);
                })
                .fetch()
                .filter(value->value.getCost()!= null)
                .collectList()
                .flatMapMany(list->{
                    ArrayList<EntryMonthRes> result = new ArrayList<>();
                    Map<String, List<EntryMonthRes>> collect = list.stream().collect(Collectors.groupingBy(EntryMonthRes::getMonth));
                    for (String s : timeList) {
                        EntryMonthRes entryMonthRes = new EntryMonthRes();
                        HashMap<String, BigDecimal> stringBigDecimalHashMap = new HashMap<>();
                        stringBigDecimalHashMap.put("totalConsumption",BigDecimal.ZERO);
                        stringBigDecimalHashMap.put("EnergyConsumptionCost",BigDecimal.ZERO);
                        entryMonthRes.setCuspPeriods(stringBigDecimalHashMap);
                        entryMonthRes.setFlatPeriods(stringBigDecimalHashMap);
                        entryMonthRes.setPeakPeriods(stringBigDecimalHashMap);
                        entryMonthRes.setValleyPeriods(stringBigDecimalHashMap);
                        if(collect.get(s) != null){
                            entryMonthRes.setMonth(s);
                            List<EntryMonthRes> costList = collect.get(s);

                            Map<Integer, List<EntryMonthRes>> collect1 = costList.stream().collect(Collectors.groupingBy(EntryMonthRes::getType));
                            for (Map.Entry<Integer, List<EntryMonthRes>> integerListEntry : collect1.entrySet()) {
                                Integer key = integerListEntry.getKey();
                                //对应月份各阶段的总能耗
                                BigDecimal totalEnergy = integerListEntry.getValue().stream().map(EntryMonthRes::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                                BigDecimal reduce = integerListEntry.getValue().stream().map(EntryMonthRes::getCost).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                if(key==1){
                                    HashMap<String, BigDecimal> hashMap = new HashMap<>();
                                    hashMap.put("totalConsumption",totalEnergy);
                                    hashMap.put("EnergyConsumptionCost",reduce);
                                    entryMonthRes.setCuspPeriods(hashMap);
                                }else if(key==2){
                                    HashMap<String, BigDecimal> hashMap = new HashMap<>();
                                    hashMap.put("totalConsumption",totalEnergy);
                                    hashMap.put("EnergyConsumptionCost",reduce);
                                    entryMonthRes.setPeakPeriods(hashMap);
                                }else if(key==3){
                                    HashMap<String, BigDecimal> hashMap = new HashMap<>();
                                    hashMap.put("totalConsumption",totalEnergy);
                                    hashMap.put("EnergyConsumptionCost",reduce);
                                    entryMonthRes.setFlatPeriods(hashMap);
                                }else if(key==4){
                                    HashMap<String, BigDecimal> hashMap = new HashMap<>();
                                    hashMap.put("totalConsumption",totalEnergy);
                                    hashMap.put("EnergyConsumptionCost",reduce);
                                    entryMonthRes.setValleyPeriods(hashMap);
                                }
                            }
                            result.add(entryMonthRes);
                        }
                    }
                    return Flux.fromIterable(result);
        });
        }
    }

    /**
     * 根据传入的时间返回以天为间隔的list
     * @param
     * @return
     */
    private List<String> getList(String start,String end){
        LocalDate dateS = LocalDate.parse(start);
        LocalDate dateE = LocalDate.parse(end);

        ArrayList<String> strings = new ArrayList<>();
        while (dateS.compareTo(dateE) <= 0){
            strings.add(dateS.toString());
            dateS = dateS.plusDays(1);
        }
        return strings;
    }


    /**成本分析
     *
     * @return
     */
    @Operation(summary = "成本分析折线图")
    @PostMapping("/cost/pic")
    public Mono<TreeMap<String, List<CostReturnRes>>> getData(@RequestBody AnalysisCostReq analysisCostReq ){
        List<String> collect = analysisCostReq.getTestAreaList().stream().map(TestAreaEntity::getId).collect(Collectors.toList());
        //时间转时间戳(开始)
        String startTime = analysisCostReq.getStartTime();
        startTime=startTime+" 00:00:00";
        long timeS = DateUtil.stringToDate(startTime, DateUtil.DATE_WITHSECOND_FORMAT).getTime();
        //时间转时间戳(结束)
        String endTime = analysisCostReq.getEndTime();
        endTime=endTime+" 23:59:59";
        long timeE = DateUtil.stringToDate(endTime, DateUtil.DATE_WITHSECOND_FORMAT).getTime();

        Long[] longs={timeS,timeE};
        TreeMap<String, List<CostReturnRes>> map = this.getMap(analysisCostReq);
        String format="%Y-%m-%d";
        if(analysisCostReq.getDimition()==1){
            format="%Y-%m";
        }
        String sql="SELECT  DATE_FORMAT( FROM_UNIXTIME( substr( gather_time, 1, 10 ), '%Y-%m-%d %H:%i:%S' ), " +
            "'"+format+"' ) as time ,sta.area_name as areaName,sum(sec.difference) as number FROM    sems_electricity_consume " +
            "sec left join sems_device_info sdi on  sdi.device_id=sec.device_id  inner join   sems_test_area sta  on sta.id=sdi.place_id   " +
            "  GROUP BY areaName,time ";
        return queryHelper
            .select(sql,CostReturnRes::new)
            .where(dsl->dsl.and("gather_time","btw",longs)
                .and("sta.id","in",collect))
            .fetch()
            .filter(i->i.getAreaName()!= null)
            .collect(Collectors.groupingBy(CostReturnRes::getTime))
            .flatMap(dataMap->{

                for (Map.Entry<String, List<CostReturnRes>> resEntry : map.entrySet()) {
                    String key = resEntry.getKey();
                    if(dataMap.get(key)!=null){
                        List<CostReturnRes> costReturnRes = dataMap.get(key);
                        List<String> nameList = costReturnRes.stream().map(CostReturnRes::getAreaName).collect(Collectors.toList());

                        List<TestAreaEntity> testAreaList = analysisCostReq.getTestAreaList();
                        //填充空值
                        for (TestAreaEntity testAreaEntity : testAreaList) {
                            if(!nameList.contains(testAreaEntity.getAreaName())){
                                CostReturnRes costReturnResExtend = new CostReturnRes();
                                costReturnResExtend.setAreaName(testAreaEntity.getAreaName());
                                costReturnResExtend.setTime(key);
                                costReturnResExtend.setNumber("0");
                                costReturnRes.add(costReturnResExtend);
                            }
                        }
                        map.put(key,costReturnRes);
                    }else {
                        ArrayList<CostReturnRes> costReturnRes1 = new ArrayList<>();
                        for (TestAreaEntity testAreaEntity : analysisCostReq.getTestAreaList()) {
                            CostReturnRes costReturnRes = new CostReturnRes();
                            costReturnRes.setTime(key);
                            costReturnRes.setAreaName(testAreaEntity.getAreaName());
                            costReturnRes.setNumber("0");
                            costReturnRes1.add(costReturnRes);
                        }
                        map.put(key,costReturnRes1);
                    }

                }
                return Mono.just(map);


            });


    }
    /**
     * 根据传入的时间以及维度生成对应的时间轴map
     * @param analysisCostReq
     * @return
     */
    private TreeMap<String,List<CostReturnRes>> getMap(AnalysisCostReq analysisCostReq){
        //根据时间生成横坐标
        TreeMap<String, List<CostReturnRes>> map = new TreeMap<>();
        if(analysisCostReq.getDimition()==0){
            //按月(月份)
            String startTime = analysisCostReq.getStartTime();
            Date dateS = DateUtil.stringToDate(startTime, DateUtil.DATE_SHORT_FORMAT);
            //下一个月开始时间
            Date dateE = DateUtil.stringToDate(analysisCostReq.getEndTime(), DateUtil.DATE_SHORT_FORMAT);

            Date comDateStart=dateS;
            while (dateE.compareTo(comDateStart)>=0){
                map.put(DateUtil.dateToString(comDateStart,DateUtil.DATE_SHORT_FORMAT),null);
                comDateStart=DateUtil.stringToDate(DateUtil.addDay(comDateStart,1),"yyyy-MM-dd");
            }
            return map;
        }else if(analysisCostReq.getDimition()==1){
            //按年
            String startTime = analysisCostReq.getStartTime();
            Date dateS = DateUtil.stringToDate(startTime, DateUtil.DATE_SHORT_FORMAT);
            //下一年月开始时间
            Date dateE = DateUtil.stringToDate(analysisCostReq.getEndTime(), DateUtil.DATE_SHORT_FORMAT);

            Date comDateStart=dateS;
            while (dateE.compareTo(comDateStart)>=0){
                map.put(DateUtil.dateToString(comDateStart,DateUtil.DATE_SHORT_YEAR_MONTH),null);
                comDateStart=DateUtil.addMonths(comDateStart,1);
            }
            return map;
        }else {
            //自定义
            String startTime = analysisCostReq.getStartTime();
            Date start = DateUtil.stringToDate(startTime, DateUtil.DATE_SHORT_FORMAT);
            String endTime = analysisCostReq.getEndTime();
            Date end = DateUtil.stringToDate(endTime, DateUtil.DATE_SHORT_FORMAT);

            Date compDate=start;
            while (end.compareTo(compDate)>=0){
                map.put(DateUtil.dateToString(compDate,DateUtil.DATE_SHORT_FORMAT),null);
                compDate=DateUtil.addDays(compDate,1);
            }
            return map;
        }
    }


    @Operation(summary = "成本分析报告")
    @PostMapping("/cost/report")
    public Flux<CostAnalysisReport> getReport(@RequestBody AnalysisCostReq analysisCostReq){

        Calendar instance = Calendar.getInstance();
        //获取当前尖峰、高峰、平段、低谷的单价
        return costConfService
            .createQuery()
            .where(CostConfigEntity::getEnergyType, "2")
            .and(CostConfigEntity::getState, 1)
            .fetch()
            .flatMap(e -> electricityIntervalService
                .createQuery()
                .where(ElectricityIntervalEntity::getCostConfigId, e.getId())
                .and(ElectricityIntervalEntity::getState, 1)
                .nest()
                .lte(ElectricityIntervalEntity::getYearStart, instance.get(Calendar.YEAR))
                .gte(ElectricityIntervalEntity::getYearEnd, instance.get(Calendar.YEAR))
                .end()
                .$like$(ElectricityIntervalEntity::getMonth, instance.get(Calendar.MONTH))
                .fetch()
                .flatMap(electricityInterval -> {
                    BigDecimal unitPrice = new BigDecimal(e.getReferencePrice());
                    BigDecimal cuspCost=BigDecimal.ZERO;
                    BigDecimal peakCost=BigDecimal.ZERO;
                    BigDecimal flatCost=BigDecimal.ZERO;
                    BigDecimal vallayCost=BigDecimal.ZERO;
                    //尖

                        List<String> reference = Arrays.asList(e.getReferenceElectricityPriceFloat()
                            .split(","));
                        if (reference.contains(String.valueOf(instance.get(Calendar.MONTH)))) {
                            cuspCost=unitPrice.add(
                                unitPrice.multiply(new BigDecimal(e.getReferencePriceFloat())
                                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
                        } else {
                            cuspCost=unitPrice.add(
                                unitPrice.multiply(new BigDecimal(e.getOtherMonthFloat())
                                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
                        }

                    //高
                        peakCost=unitPrice.add(
                            unitPrice.multiply(new BigDecimal(e.getPeakOnReferenceFloat())
                                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
                    //平
                    flatCost=unitPrice;

                    //谷
                    vallayCost=unitPrice.subtract(
                            unitPrice.multiply(new BigDecimal(e.getLowOnReferenceFloat())
                                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));

                    return this.getReportExtend(analysisCostReq,cuspCost,peakCost,flatCost,vallayCost);
                })
            );
    }

    public Flux<CostAnalysisReport> getReportExtend(AnalysisCostReq analysisCostReq,BigDecimal cuspCost,BigDecimal peakCost,BigDecimal flatCost,BigDecimal valleyCost){
        List<TestAreaEntity> testAreaList = analysisCostReq.getTestAreaList();
        if(analysisCostReq.getDimition()==1){

            String year = analysisCostReq.getStartTime().substring(0,4);
            return Flux.fromIterable(testAreaList)
                .flatMap(value->{


                    ArrayList<TestAreaEntity> testAreaEntities = new ArrayList<>();
                    testAreaEntities.add(value);

                    CostAnalysisReport costAnalysisReport = new CostAnalysisReport();
                    costAnalysisReport.setAreaName(value.getAreaName());
                    costAnalysisReport.setTime(year);

                    AnalysisCostReq analysisCostReq1 = new AnalysisCostReq();
                    BeanUtils.copyProperties(analysisCostReq,analysisCostReq1);
                    analysisCostReq1.setTestAreaList(testAreaEntities);
                    return this.yearAnalysis(analysisCostReq1)
                        .collectList()
                        .flatMapMany(list->{
                            //尖
                            BigDecimal _1total =BigDecimal.ZERO;
                            BigDecimal _1cost =BigDecimal.ZERO;
                            //峰
                            BigDecimal _2cost =BigDecimal.ZERO;
                            BigDecimal _2use =BigDecimal.ZERO;
                            //平
                            BigDecimal _3cost =BigDecimal.ZERO;
                            BigDecimal _3use =BigDecimal.ZERO;
                            //谷
                            BigDecimal _4cost =BigDecimal.ZERO;
                            BigDecimal _4use =BigDecimal.ZERO;
                            //尖峰
                            List<Map> _1collect = list.stream().map(EntryMonthRes::getCuspPeriods).collect(Collectors.toList());
                            for (Map map : _1collect) {
                                _1total = _1total.add(BigDecimal.valueOf(Double.parseDouble(map.get("totalConsumption").toString())));
                                _1cost = _1cost.add(BigDecimal.valueOf(Double.parseDouble(map.get("EnergyConsumptionCost").toString())));
                            }
                            //高峰
                            List<Map> _2collect = list.stream().map(EntryMonthRes::getPeakPeriods).collect(Collectors.toList());
                            for (Map map : _2collect) {
                                _2use = _2use.add(BigDecimal.valueOf(Double.parseDouble(map.get("totalConsumption").toString())));
                                _2cost = _2cost.add(BigDecimal.valueOf(Double.parseDouble(map.get("EnergyConsumptionCost").toString())));
                            }
                            //平段
                            List<Map> _3collect = list.stream().map(EntryMonthRes::getFlatPeriods).collect(Collectors.toList());
                            for (Map map : _3collect) {
                                _3use = _3use.add(BigDecimal.valueOf(Double.parseDouble(map.get("totalConsumption").toString())));
                                _3cost = _3cost.add(BigDecimal.valueOf(Double.parseDouble(map.get("EnergyConsumptionCost").toString())));
                            }
                            //低谷
                            List<Map> _4collect = list.stream().map(EntryMonthRes::getValleyPeriods).collect(Collectors.toList());
                            for (Map map : _4collect) {
                                _4use = _4use.add(BigDecimal.valueOf(Double.parseDouble(map.get("totalConsumption").toString())));
                                _4cost = _4cost.add(BigDecimal.valueOf(Double.parseDouble(map.get("EnergyConsumptionCost").toString())));
                            }

                            if(_1total.compareTo(BigDecimal.ZERO)!=0){
                                _2cost=peakCost.multiply(_1total);
                                _3cost=flatCost.multiply(_1total);
                                _4cost=valleyCost.multiply(_1total);
                                costAnalysisReport.setPeakUse(_1total.setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setPeakCost(_1cost.setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setHightUse(_2use);
                                costAnalysisReport.setHighCost(_1cost.subtract(_2cost).setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setFlatUse(_3use);
                                costAnalysisReport.setFlatCost(_1cost.subtract(_3cost).setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setLowUse(_4use);
                                costAnalysisReport.setLowCost(_1cost.subtract(_4cost).setScale(2,RoundingMode.HALF_UP));
                                return Mono.just(costAnalysisReport);
                            }else if(_2use.compareTo(BigDecimal.ZERO)!=0){
                                _3cost=flatCost.multiply(_2use);
                                _4cost=valleyCost.multiply(_2use);
                                costAnalysisReport.setPeakUse(null);
                                costAnalysisReport.setPeakCost(null);
                                costAnalysisReport.setHightUse(_2use);
                                costAnalysisReport.setHighCost(_2cost.setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setFlatUse(_3use);
                                costAnalysisReport.setFlatCost(_2cost.subtract(_3cost).setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setLowUse(_4use);
                                costAnalysisReport.setLowCost(_2cost.subtract(_4cost).setScale(2,RoundingMode.HALF_UP));
                                return Mono.just(costAnalysisReport);
                            }else if(_3use.compareTo(BigDecimal.ZERO)!=0){
                                _4cost=valleyCost.multiply(_2use);
                                costAnalysisReport.setPeakUse(null);
                                costAnalysisReport.setPeakCost(null);
                                costAnalysisReport.setHightUse(null);
                                costAnalysisReport.setHighCost(null);
                                costAnalysisReport.setFlatUse(_3use);
                                costAnalysisReport.setFlatCost(_3cost.setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setLowUse(_4use);
                                costAnalysisReport.setLowCost(_3cost.subtract(_4cost).setScale(2,RoundingMode.HALF_UP));
                                return Mono.just(costAnalysisReport);
                            }else if(_4use.compareTo(BigDecimal.ZERO)!=0){
                                costAnalysisReport.setPeakUse(null);
                                costAnalysisReport.setPeakCost(null);
                                costAnalysisReport.setHightUse(null);
                                costAnalysisReport.setHighCost(null);
                                costAnalysisReport.setFlatUse(null);
                                costAnalysisReport.setFlatCost(null);
                                costAnalysisReport.setLowUse(_4use);
                                costAnalysisReport.setLowCost(_4cost.setScale(2,RoundingMode.HALF_UP));
                                return Mono.just(costAnalysisReport);
                            }else {
                                return Mono.empty();
                            }
                        });
                });
        }else if(analysisCostReq.getDimition()==0) {
            String month = analysisCostReq.getStartTime().substring(0,7);
            return Flux.fromIterable(testAreaList)
                .flatMap(value->{

                    ArrayList<TestAreaEntity> testAreaEntities = new ArrayList<>();
                    testAreaEntities.add(value);

                    CostAnalysisReport costAnalysisReport = new CostAnalysisReport();
                    costAnalysisReport.setAreaName(value.getAreaName());
                    costAnalysisReport.setTime(month);


                    AnalysisCostReq analysisCostReq1 = new AnalysisCostReq();
                    BeanUtils.copyProperties(analysisCostReq,analysisCostReq1);
                    analysisCostReq1.setTestAreaList(testAreaEntities);
                    return this.yearAnalysis(analysisCostReq1)
                        .collectList()
                        .flatMapMany(list->{
                            //尖
                            BigDecimal _1total =BigDecimal.ZERO;
                            BigDecimal _1cost =BigDecimal.ZERO;
                            //峰
                            BigDecimal _2cost =BigDecimal.ZERO;
                            BigDecimal _2use =BigDecimal.ZERO;
                            //平
                            BigDecimal _3cost =BigDecimal.ZERO;
                            BigDecimal _3use =BigDecimal.ZERO;
                            //谷
                            BigDecimal _4cost =BigDecimal.ZERO;
                            BigDecimal _4use =BigDecimal.ZERO;
                            //尖峰
                            List<Map> _1collect = list.stream().map(EntryMonthRes::getCuspPeriods).collect(Collectors.toList());
                            for (Map map : _1collect) {
                                _1total = _1total.add(BigDecimal.valueOf(Double.parseDouble(map.get("totalConsumption").toString())));
                                _1cost = _1cost.add(BigDecimal.valueOf(Double.parseDouble(map.get("EnergyConsumptionCost").toString())));
                            }
                            //高峰
                            List<Map> _2collect = list.stream().map(EntryMonthRes::getPeakPeriods).collect(Collectors.toList());
                            for (Map map : _2collect) {
                                _2use = _2use.add(BigDecimal.valueOf(Double.parseDouble(map.get("totalConsumption").toString())));
                                _2cost = _2cost.add(BigDecimal.valueOf(Double.parseDouble(map.get("EnergyConsumptionCost").toString())));
                            }
                            //平段
                            List<Map> _3collect = list.stream().map(EntryMonthRes::getFlatPeriods).collect(Collectors.toList());
                            for (Map map : _3collect) {
                                _3use = _3use.add(BigDecimal.valueOf(Double.parseDouble(map.get("totalConsumption").toString())));
                                _3cost = _3cost.add(BigDecimal.valueOf(Double.parseDouble(map.get("EnergyConsumptionCost").toString())));
                            }
                            //低谷
                            List<Map> _4collect = list.stream().map(EntryMonthRes::getValleyPeriods).collect(Collectors.toList());
                            for (Map map : _4collect) {
                                _4use = _4use.add(BigDecimal.valueOf(Double.parseDouble(map.get("totalConsumption").toString())));
                                _4cost = _4cost.add(BigDecimal.valueOf(Double.parseDouble(map.get("EnergyConsumptionCost").toString())));
                            }

                            if(_1total.compareTo(BigDecimal.ZERO)!=0){
                                _2cost=peakCost.multiply(_1total);
                                _3cost=flatCost.multiply(_1total);
                                _4cost=valleyCost.multiply(_1total);
                                costAnalysisReport.setPeakUse(_1total.setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setPeakCost(_1cost.setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setHightUse(_2use);
                                costAnalysisReport.setHighCost(_1cost.subtract(_2cost).setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setFlatUse(_3use);
                                costAnalysisReport.setFlatCost(_1cost.subtract(_3cost).setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setLowUse(_4use);
                                costAnalysisReport.setLowCost(_1cost.subtract(_4cost).setScale(2,RoundingMode.HALF_UP));
                                return Mono.just(costAnalysisReport);
                            }else if(_2use.compareTo(BigDecimal.ZERO)!=0){
                                _3cost=flatCost.multiply(_2use);
                                _4cost=valleyCost.multiply(_2use);
                                costAnalysisReport.setPeakUse(null);
                                costAnalysisReport.setPeakCost(null);
                                costAnalysisReport.setHightUse(_2use);
                                costAnalysisReport.setHighCost(_2cost.setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setFlatUse(_3use);
                                costAnalysisReport.setFlatCost(_2cost.subtract(_3cost).setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setLowUse(_4use);
                                costAnalysisReport.setLowCost(_2cost.subtract(_4cost).setScale(2,RoundingMode.HALF_UP));
                                return Mono.just(costAnalysisReport);
                            }else if(_3use.compareTo(BigDecimal.ZERO)!=0){
                                _4cost=valleyCost.multiply(_2use);
                                costAnalysisReport.setPeakUse(null);
                                costAnalysisReport.setPeakCost(null);
                                costAnalysisReport.setHightUse(null);
                                costAnalysisReport.setHighCost(null);
                                costAnalysisReport.setFlatUse(_3use);
                                costAnalysisReport.setFlatCost(_3cost.setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setLowUse(_4use);
                                costAnalysisReport.setLowCost(_3cost.subtract(_4cost).setScale(2,RoundingMode.HALF_UP));
                                return Mono.just(costAnalysisReport);
                            }else if(_4use.compareTo(BigDecimal.ZERO)!=0){
                                costAnalysisReport.setPeakUse(null);
                                costAnalysisReport.setPeakCost(null);
                                costAnalysisReport.setHightUse(null);
                                costAnalysisReport.setHighCost(null);
                                costAnalysisReport.setFlatUse(null);
                                costAnalysisReport.setFlatCost(null);
                                costAnalysisReport.setLowUse(_4use);
                                costAnalysisReport.setLowCost(_4cost.setScale(2,RoundingMode.HALF_UP));
                                return Mono.just(costAnalysisReport);
                            }else {
                                return Mono.empty();
                            }
                        });


                });
        }else {
            String time = analysisCostReq.getStartTime()+"~"+analysisCostReq.getEndTime();
            return Flux.fromIterable(testAreaList)
                .flatMap(value->{

                    ArrayList<TestAreaEntity> testAreaEntities = new ArrayList<>();
                    testAreaEntities.add(value);

                    CostAnalysisReport costAnalysisReport = new CostAnalysisReport();
                    costAnalysisReport.setAreaName(value.getAreaName());
                    costAnalysisReport.setTime(time);

                    AnalysisCostReq analysisCostReq1 = new AnalysisCostReq();
                    BeanUtils.copyProperties(analysisCostReq,analysisCostReq1);
                    analysisCostReq1.setTestAreaList(testAreaEntities);
                    return this.yearAnalysis(analysisCostReq1)
                        .collectList()
                        .flatMapMany(list->{
                            //尖
                            BigDecimal _1total =BigDecimal.ZERO;
                            BigDecimal _1cost =BigDecimal.ZERO;
                            //峰
                            BigDecimal _2cost =BigDecimal.ZERO;
                            BigDecimal _2use =BigDecimal.ZERO;
                            //平
                            BigDecimal _3cost =BigDecimal.ZERO;
                            BigDecimal _3use =BigDecimal.ZERO;
                            //谷
                            BigDecimal _4cost =BigDecimal.ZERO;
                            BigDecimal _4use =BigDecimal.ZERO;
                            //尖峰
                            List<Map> _1collect = list.stream().map(EntryMonthRes::getCuspPeriods).collect(Collectors.toList());
                            for (Map map : _1collect) {
                                _1total = _1total.add(BigDecimal.valueOf(Double.parseDouble(map.get("totalConsumption").toString())));
                                _1cost = _1cost.add(BigDecimal.valueOf(Double.parseDouble(map.get("EnergyConsumptionCost").toString())));
                            }
                            //高峰
                            List<Map> _2collect = list.stream().map(EntryMonthRes::getPeakPeriods).collect(Collectors.toList());
                            for (Map map : _2collect) {
                                _2use = _2use.add(BigDecimal.valueOf(Double.parseDouble(map.get("totalConsumption").toString())));
                                _2cost = _2cost.add(BigDecimal.valueOf(Double.parseDouble(map.get("EnergyConsumptionCost").toString())));
                            }
                            //平段
                            List<Map> _3collect = list.stream().map(EntryMonthRes::getFlatPeriods).collect(Collectors.toList());
                            for (Map map : _3collect) {
                                _3use = _3use.add(BigDecimal.valueOf(Double.parseDouble(map.get("totalConsumption").toString())));
                                _3cost = _3cost.add(BigDecimal.valueOf(Double.parseDouble(map.get("EnergyConsumptionCost").toString())));
                            }
                            //低谷
                            List<Map> _4collect = list.stream().map(EntryMonthRes::getValleyPeriods).collect(Collectors.toList());
                            for (Map map : _4collect) {
                                _4use = _4use.add(BigDecimal.valueOf(Double.parseDouble(map.get("totalConsumption").toString())));
                                _4cost = _4cost.add(BigDecimal.valueOf(Double.parseDouble(map.get("EnergyConsumptionCost").toString())));
                            }

                            if(_1total.compareTo(BigDecimal.ZERO)!=0){
                                _2cost=peakCost.multiply(_1total);
                                _3cost=flatCost.multiply(_1total);
                                _4cost=valleyCost.multiply(_1total);
                                costAnalysisReport.setPeakUse(_1total.setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setPeakCost(_1cost.setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setHightUse(_2use);
                                costAnalysisReport.setHighCost(_1cost.subtract(_2cost).setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setFlatUse(_3use);
                                costAnalysisReport.setFlatCost(_1cost.subtract(_3cost).setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setLowUse(_4use);
                                costAnalysisReport.setLowCost(_1cost.subtract(_4cost).setScale(2,RoundingMode.HALF_UP));
                                return Mono.just(costAnalysisReport);
                            }else if(_2use.compareTo(BigDecimal.ZERO)!=0){
                                _3cost=flatCost.multiply(_2use);
                                _4cost=valleyCost.multiply(_2use);
                                costAnalysisReport.setPeakUse(null);
                                costAnalysisReport.setPeakCost(null);
                                costAnalysisReport.setHightUse(_2use);
                                costAnalysisReport.setHighCost(_2cost.setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setFlatUse(_3use);
                                costAnalysisReport.setFlatCost(_2cost.subtract(_3cost).setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setLowUse(_4use);
                                costAnalysisReport.setLowCost(_2cost.subtract(_4cost).setScale(2,RoundingMode.HALF_UP));
                                return Mono.just(costAnalysisReport);
                            }else if(_3use.compareTo(BigDecimal.ZERO)!=0){
                                _4cost=valleyCost.multiply(_2use);
                                costAnalysisReport.setPeakUse(null);
                                costAnalysisReport.setPeakCost(null);
                                costAnalysisReport.setHightUse(null);
                                costAnalysisReport.setHighCost(null);
                                costAnalysisReport.setFlatUse(_3use);
                                costAnalysisReport.setFlatCost(_3cost.setScale(2,RoundingMode.HALF_UP));
                                costAnalysisReport.setLowUse(_4use);
                                costAnalysisReport.setLowCost(_3cost.subtract(_4cost).setScale(2,RoundingMode.HALF_UP));
                                return Mono.just(costAnalysisReport);
                            }else if(_4use.compareTo(BigDecimal.ZERO)!=0){
                                costAnalysisReport.setPeakUse(null);
                                costAnalysisReport.setPeakCost(null);
                                costAnalysisReport.setHightUse(null);
                                costAnalysisReport.setHighCost(null);
                                costAnalysisReport.setFlatUse(null);
                                costAnalysisReport.setFlatCost(null);
                                costAnalysisReport.setLowUse(_4use);
                                costAnalysisReport.setLowCost(_4cost.setScale(2,RoundingMode.HALF_UP));
                                return Mono.just(costAnalysisReport);
                            }else {
                                return Mono.empty();
                            }
                        });
                });
        }
    }



}
