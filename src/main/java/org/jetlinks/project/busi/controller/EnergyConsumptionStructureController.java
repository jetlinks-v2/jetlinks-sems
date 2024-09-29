package org.jetlinks.project.busi.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.utils.StringUtils;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.project.busi.entity.res.BoardReportRes;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.*;
import org.jetlinks.project.busi.entity.req.EnergyStructureReq;
import org.jetlinks.project.busi.entity.req.EnergyTrendsReq;
import org.jetlinks.project.busi.entity.res.*;
import org.jetlinks.project.busi.service.*;
import org.jetlinks.project.busi.utils.DateUtil;
import org.springframework.expression.Expression;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/sems/energy/structure")
@AllArgsConstructor
@Getter
@Tag(name = "能耗构成1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "energy-structure", name = "能耗构成")
public class EnergyConsumptionStructureController {


    private final QueryHelper queryHelper;
    private final TestConfigService testConfigService;
    private final CostConfService costConfService;
    private final CalculationService calculationService;
    private final AreaInfoService areaInfoService;
    private final DeviceService deviceService;
    private final ElectricityConsumeService electricityConsumeService;
    private final LevelConfigService levelConfigService;
    private final CostNameAndRemarkService costNameAndRemarkService;


    @Operation(summary = "能耗看板趋势")
    @PostMapping("/energyBoard/tred")
    @Authorize(ignore = true)
    public Mono<HashMap> get(@RequestBody EnergyTrendsReq energyTrendsReq){
        HashMap<String, Object> resultMap = new HashMap<>();
        //尖峰平谷初始化
        resultMap.put("cuspPeriods",BigDecimal.ZERO);
        resultMap.put("peakPeriods",BigDecimal.ZERO);
        resultMap.put("flatPeriods",BigDecimal.ZERO);
        resultMap.put("valleyPeriods",BigDecimal.ZERO);
        int i = DateUtil.daysOfTwo(energyTrendsReq.getStartDate(),energyTrendsReq.getEndDate());
        String format=null;
        if (i<=1){
        format="%Y-%m-%d %H:00";
    }else if(i<=31){
        format="%Y-%m-%d";
    }else if(i<=365)  {
        format="%Y-%m";
    }else {
            format="%Y";
        }

        List<EnergyTredResExtend> energyByDate = this.getTimeByDate(energyTrendsReq.getStartDate(), energyTrendsReq.getEndDate(),i);
        Long[] dates={energyTrendsReq.getStartDate().getTime(),energyTrendsReq.getEndDate().getTime()};

        if("electricity".equals(energyTrendsReq.getType())){
            //电
            if(i<=1){
                String hourSql = "SELECT DATE_FORMAT(FROM_UNIXTIME((se.gather_time+60*60*1000) / 1000), '"+format+"') AS time,\n" +
                    "       SUM(CASE WHEN se.periods_type = 1 THEN se.difference ELSE 0 END) AS cuspNumber,\n" +
                    "       SUM(CASE WHEN se.periods_type = 2 THEN se.difference ELSE 0 END) AS peakNumber,\n" +
                    "       SUM(CASE WHEN se.periods_type = 3 THEN se.difference ELSE 0 END) AS flatNumber,\n" +
                    "       SUM(CASE WHEN se.periods_type = 4 THEN se.difference ELSE 0 END) AS valleyNumber\n" +
                    "FROM sems_electricity_consume se \n" +
                    "WHERE se.device_id='0'\n"+
                    "GROUP BY \n" +
                    "DATE_FORMAT(FROM_UNIXTIME((se.gather_time+60*60*1000) / 1000), '"+format+"');";

                return queryHelper
                    .select(hourSql,EnergyTredResExtend::new)
                    .where(dsl->dsl.and("gather_time","btw",dates).noPaging())
                    .fetch()
                    .collectList()
                    .flatMap(thred-> {
                        //按天显示当前小时
                        if(!thred.isEmpty() && i<=1){
                            EnergyTredResExtend energyTrendRes = thred.get(thred.size() - 1);
                            String time = energyTrendRes.getTime();

                            Date date = DateUtil.stringToDate(time,DateUtil.DATE_WITHHOUR_FORMAT);
                            if(date.compareTo(energyTrendsReq.getEndDate())>0){
                                thred.remove(thred.size() - 1);
                                EnergyTredResExtend energyTrendRes1 = new EnergyTredResExtend();
                                energyTrendRes1.setTime(DateUtil.dateToString(energyTrendsReq.getEndDate(),DateUtil.DATE_WITHMINUTE_FORMAT));
                                energyTrendRes1.setValleyNumber(energyTrendRes.getValleyNumber().setScale(2,RoundingMode.HALF_UP));
                                energyTrendRes1.setPeakNumber(energyTrendRes.getPeakNumber().setScale(2,RoundingMode.HALF_UP));
                                energyTrendRes1.setFlatNumber(energyTrendRes.getFlatNumber().setScale(2,RoundingMode.HALF_UP));
                                energyTrendRes1.setCuspNumber(energyTrendRes.getCuspNumber().setScale(2,RoundingMode.HALF_UP));
                                thred.add(energyTrendRes1);
                            }

                        }

                        Map<String, EnergyTredResExtend> collect = thred.stream().collect(Collectors.toMap(EnergyTredResExtend::getTime, j -> j));
                        ArrayList<EnergyTredResExtend> result = new ArrayList<>();
                        BigDecimal cus=BigDecimal.ZERO;
                        BigDecimal peak=BigDecimal.ZERO;
                        BigDecimal flat=BigDecimal.ZERO;
                        BigDecimal valley=BigDecimal.ZERO;
                        for (EnergyTredResExtend energyTrendRes : energyByDate) {
                            if(collect.get(energyTrendRes.getTime())!= null){

                                result.add(collect.get(energyTrendRes.getTime()));
                                cus=cus.add(collect.get(energyTrendRes.getTime()).getCuspNumber()==null?BigDecimal.ZERO:collect.get(energyTrendRes.getTime()).getCuspNumber());
                                peak=peak.add(collect.get(energyTrendRes.getTime()).getPeakNumber()==null?BigDecimal.ZERO:collect.get(energyTrendRes.getTime()).getPeakNumber());
                                flat=flat.add(collect.get(energyTrendRes.getTime()).getFlatNumber()==null?BigDecimal.ZERO:collect.get(energyTrendRes.getTime()).getFlatNumber());
                                valley=valley.add(collect.get(energyTrendRes.getTime()).getValleyNumber()==null?BigDecimal.ZERO:collect.get(energyTrendRes.getTime()).getValleyNumber());
                            }else {
                                EnergyTredResExtend energyTrendRes2 = new EnergyTredResExtend();
                                energyTrendRes2.setTime(energyTrendRes.getTime());
                                energyTrendRes2.setValleyNumber(BigDecimal.ZERO);
                                energyTrendRes2.setPeakNumber(BigDecimal.ZERO);
                                energyTrendRes2.setFlatNumber(BigDecimal.ZERO);
                                energyTrendRes2.setCuspNumber(BigDecimal.ZERO);
                                result.add(energyTrendRes2);
                            }
                        }
                        resultMap.put("list",result);
                        resultMap.put("cuspPeriods",cus);
                        resultMap.put("peakPeriods",peak);
                        resultMap.put("flatPeriods",flat);
                        resultMap.put("valleyPeriods",valley);
                        return Mono.just(resultMap);
                    });

            }else if(i<=31) {
                String dayAndMonthsql = "SELECT DATE_FORMAT(FROM_UNIXTIME((se.gather_time) / 1000), '"+format+"') AS time,\n" +
                    "       SUM(CASE WHEN se.periods_type = 1 THEN se.difference ELSE 0 END) AS cuspNumber,\n" +
                    "       SUM(CASE WHEN se.periods_type = 2 THEN se.difference ELSE 0 END) AS peakNumber,\n" +
                    "       SUM(CASE WHEN se.periods_type = 3 THEN se.difference ELSE 0 END) AS flatNumber,\n" +
                    "       SUM(CASE WHEN se.periods_type = 4 THEN se.difference ELSE 0 END) AS valleyNumber\n" +
                    "FROM sems_electricity_consume se \n" +
                    "WHERE se.device_id='0'\n"+
                    "GROUP BY \n" +
                    "DATE_FORMAT(FROM_UNIXTIME((se.gather_time) / 1000), '"+format+"');";
                return queryHelper
                    .select(dayAndMonthsql,EnergyTredResExtend::new)
                    .where(dsl->dsl.and("gather_time","btw",dates).noPaging())
                    .fetch()
                    .collectMap(EnergyTredResExtend::getTime,energyTredResExtend->energyTredResExtend)
                    .flatMap(thred-> {
                        ArrayList<EnergyTredResExtend> result = new ArrayList<>();
                        BigDecimal cus=BigDecimal.ZERO;
                        BigDecimal peak=BigDecimal.ZERO;
                        BigDecimal flat=BigDecimal.ZERO;
                        BigDecimal valley=BigDecimal.ZERO;
                        for (EnergyTredResExtend energyTrendRes : energyByDate) {
                            if(thred.get(energyTrendRes.getTime())!= null){
                                result.add(thred.get(energyTrendRes.getTime()));
                                cus=cus.add(thred.get(energyTrendRes.getTime()).getCuspNumber()==null?BigDecimal.ZERO:thred.get(energyTrendRes.getTime()).getCuspNumber());
                                peak=peak.add(thred.get(energyTrendRes.getTime()).getPeakNumber()==null?BigDecimal.ZERO:thred.get(energyTrendRes.getTime()).getPeakNumber());
                                flat=flat.add(thred.get(energyTrendRes.getTime()).getFlatNumber()==null?BigDecimal.ZERO:thred.get(energyTrendRes.getTime()).getFlatNumber());
                                valley=valley.add(thred.get(energyTrendRes.getTime()).getValleyNumber()==null?BigDecimal.ZERO:thred.get(energyTrendRes.getTime()).getValleyNumber());
                            }else {
                                EnergyTredResExtend energyTrendRes2 = new EnergyTredResExtend();
                                energyTrendRes2.setTime(energyTrendRes.getTime());
                                energyTrendRes2.setValleyNumber(BigDecimal.ZERO);
                                energyTrendRes2.setPeakNumber(BigDecimal.ZERO);
                                energyTrendRes2.setFlatNumber(BigDecimal.ZERO);
                                energyTrendRes2.setCuspNumber(BigDecimal.ZERO);
                                result.add(energyTrendRes2);
                            }
                        }
                        resultMap.put("list",result);
                        resultMap.put("cuspPeriods",cus);
                        resultMap.put("peakPeriods",peak);
                        resultMap.put("flatPeriods",flat);
                        resultMap.put("valleyPeriods",valley);
                        return Mono.just(resultMap);
                    });
            }else {
                String dayAndMonthsql = "SELECT DATE_FORMAT(FROM_UNIXTIME((se.gather_time) / 1000), '"+format+"') AS time,\n" +
                    "       SUM(CASE WHEN se.periods_type = 1 THEN se.difference ELSE 0 END) AS cuspNumber,\n" +
                    "       SUM(CASE WHEN se.periods_type = 2 THEN se.difference ELSE 0 END) AS peakNumber,\n" +
                    "       SUM(CASE WHEN se.periods_type = 3 THEN se.difference ELSE 0 END) AS flatNumber,\n" +
                    "       SUM(CASE WHEN se.periods_type = 4 THEN se.difference ELSE 0 END) AS valleyNumber\n" +
                    "FROM sems_electricity_consume se \n" +
                    "WHERE se.device_id='0'\n"+
                    "GROUP BY \n" +
                    "DATE_FORMAT(FROM_UNIXTIME((se.gather_time) / 1000), '"+format+"');";
                return queryHelper
                    .select(dayAndMonthsql,EnergyTredResExtend::new)
                    .where(dsl->dsl.and("gather_time","btw",dates).noPaging())
                    .fetch()
                    .collectMap(EnergyTredResExtend::getTime,energyTredResExtend->energyTredResExtend)
                    .flatMap(thred-> {
                        ArrayList<EnergyTredResExtend> result = new ArrayList<>();
                        BigDecimal cus=BigDecimal.ZERO;
                        BigDecimal peak=BigDecimal.ZERO;
                        BigDecimal flat=BigDecimal.ZERO;
                        BigDecimal valley=BigDecimal.ZERO;
                        for (EnergyTredResExtend energyTrendRes : energyByDate) {
                            BigDecimal total=BigDecimal.ZERO;
                            if(thred.get(energyTrendRes.getTime())!= null){
                                EnergyTredResExtend energy = thred.get(energyTrendRes.getTime());
                                cus=cus.add(energy.getCuspNumber()==null?BigDecimal.ZERO:energy.getCuspNumber());
                                peak=peak.add(energy.getPeakNumber()==null?BigDecimal.ZERO:energy.getPeakNumber());
                                flat=flat.add(energy.getFlatNumber()==null?BigDecimal.ZERO:energy.getFlatNumber());
                                valley=valley.add(energy.getValleyNumber()==null?BigDecimal.ZERO:energy.getValleyNumber());
                                total=total.add(cus).add(peak).add(flat).add(valley);
                                EnergyTredResExtend energyTredResExtend = thred.get(energyTrendRes.getTime());
                                energyTredResExtend.setNum(total);
                                result.add(energyTredResExtend);
                            }else {
                                EnergyTredResExtend energyTrendRes2 = new EnergyTredResExtend();
                                energyTrendRes2.setTime(energyTrendRes.getTime());
                                energyTrendRes2.setValleyNumber(BigDecimal.ZERO);
                                energyTrendRes2.setPeakNumber(BigDecimal.ZERO);
                                energyTrendRes2.setFlatNumber(BigDecimal.ZERO);
                                energyTrendRes2.setCuspNumber(BigDecimal.ZERO);
                                energyTrendRes2.setNum(BigDecimal.ZERO);
                                result.add(energyTrendRes2);
                            }
                        }
                        resultMap.put("list",result);
                        resultMap.put("cuspPeriods",cus);
                        resultMap.put("peakPeriods",peak);
                        resultMap.put("flatPeriods",flat);
                        resultMap.put("valleyPeriods",valley);
                        return Mono.just(resultMap);
                    });
            }
        }else {
            String sql=null;
            if(i<=1){
                 sql="SELECT DATE_FORMAT(FROM_UNIXTIME((t.gather_time+1000*60*60) / 1000), '"+format+"') AS time, sum(t.difference) as num,max(difference) as peakNum,min(difference) as lowNum from sems_"+energyTrendsReq.getType()+"_consume t where t.device_id='0' and difference <>0  GROUP BY \n" +
                    "            DATE_FORMAT(FROM_UNIXTIME((t.gather_time+1000*60*60) / 1000), '"+format+"')";
            }else {
                sql="SELECT DATE_FORMAT(FROM_UNIXTIME((t.gather_time) / 1000), '"+format+"') AS time, sum(t.difference) as num,max(difference) as peakNum,min(difference) as lowNum from sems_"+energyTrendsReq.getType()+"_consume t where t.device_id='0' and difference <>0  GROUP BY \n" +
                    "            DATE_FORMAT(FROM_UNIXTIME((t.gather_time) / 1000), '"+format+"')";
            }


            return queryHelper
                .select(sql,EnergyTredResExtend::new)
                .where(dsl->dsl.and("gather_time","btw",dates).noPaging())
                .fetch()
                .collectList()
                .flatMap(thred-> {
                    //按天显示当前小时
                    if(!thred.isEmpty() && i<=1){
                        EnergyTredResExtend energyTrendRes = thred.get(thred.size() - 1);
                        String time = energyTrendRes.getTime();
                        thred.remove(thred.size() - 1);
                        Date date = DateUtil.stringToDate(time,DateUtil.DATE_WITHHOUR_FORMAT);
                        if(date.compareTo(energyTrendsReq.getEndDate())>0){
                            EnergyTredResExtend energyTrendRes1 = new EnergyTredResExtend();
                            energyTrendRes1.setTime(DateUtil.dateToString(energyTrendsReq.getEndDate(),DateUtil.DATE_WITHMINUTE_FORMAT));
                            energyTrendRes1.setPeakNum(energyTrendRes.getPeakNum());
                            energyTrendRes1.setLowNum(energyTrendRes.getLowNum());
                            energyTrendRes1.setNum(energyTrendRes.getNum());
                            thred.add(energyTrendRes1);
                        }

                    }

                    Map<String, EnergyTredResExtend> collect = thred.stream().collect(Collectors.toMap(EnergyTredResExtend::getTime, j -> j));
                    ArrayList<EnergyTredResExtend> result = new ArrayList<>();
                    for (EnergyTredResExtend energyTrendRes : energyByDate) {
                        if(collect.get(energyTrendRes.getTime())!= null){

                            result.add(collect.get(energyTrendRes.getTime()));
                        }else {
                            EnergyTredResExtend energyTrendRes2 = new EnergyTredResExtend();
                            energyTrendRes2.setTime(energyTrendRes.getTime());
                            energyTrendRes2.setLowNum(BigDecimal.ZERO);
                            energyTrendRes2.setPeakNum(BigDecimal.ZERO);
                            energyTrendRes2.setNum(BigDecimal.ZERO);
                            result.add(energyTrendRes2);
                        }
                    }
                        BigDecimal min = result.stream().map(EnergyTredResExtend::getNum).filter(nu -> nu.compareTo(BigDecimal.ZERO) > 0).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                        BigDecimal max = result.stream().map(EnergyTredResExtend::getNum).filter(nu -> nu.compareTo(BigDecimal.ZERO) > 0).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                        resultMap.put("max",max);
                        resultMap.put("min",min);
                        resultMap.put("list",result);
                        return Mono.just(resultMap);
                });

        }
}



    /**
     * 根据时间段获取时间轴
     * @param startDate
     * @param endDate
     * @return
     */
    public List<EnergyTredResExtend> getTimeByDate(Date startDate,Date endDate,int day){
        ArrayList<EnergyTredResExtend> result = new ArrayList<>();
        if(day<=1){
            Date useStart=startDate;
            while (useStart.compareTo(endDate)<=0 || useStart.compareTo(DateUtil.addHour(DateUtil.stringToDate(DateUtil.dateToString(endDate,DateUtil.DATE_WITHHOUR_FORMAT),DateUtil.DATE_WITHHOUR_FORMAT),1))==0){
                if(useStart.compareTo(endDate)>0){
                    EnergyTredResExtend energyTrendRes = new EnergyTredResExtend();
                    energyTrendRes.setTime(DateUtil.dateToString(endDate,DateUtil.DATE_WITHMINUTE_FORMAT));
                    result.add(energyTrendRes);
                    return result;
                }else if(useStart.compareTo(endDate)==0) {
                    EnergyTredResExtend energyTrendRes = new EnergyTredResExtend();
                    energyTrendRes.setTime(DateUtil.dateToString(useStart,"yyyy-MM-dd HH:00"));
                    result.add(energyTrendRes);
                    return result;
                }else {
                    EnergyTredResExtend energyTrendRes = new EnergyTredResExtend();
                    energyTrendRes.setTime(DateUtil.dateToString(useStart,"yyyy-MM-dd HH:00"));
                    useStart=DateUtil.addHour(useStart,1);
                    result.add(energyTrendRes);
                }

            }
        }else if(day<=31){
            Date useStart=startDate;
            while (useStart.compareTo(endDate)<=0){
                EnergyTredResExtend energyTrendRes = new EnergyTredResExtend();
                energyTrendRes.setTime(DateUtil.dateToString(useStart,DateUtil.DATE_SHORT_FORMAT));
                useStart=DateUtil.addDays(useStart,1);
                result.add(energyTrendRes);
            }
        }else if(day<=365){
            Date useStart=startDate;
            while (useStart.compareTo(endDate)<=0){
                EnergyTredResExtend energyTrendRes = new EnergyTredResExtend();
                energyTrendRes.setTime(DateUtil.dateToString(useStart,DateUtil.DATE_SHORT_YEAR_MONTH));
                useStart=DateUtil.addMonths(useStart,1);
                result.add(energyTrendRes);
            }
        }
        else {
            Date useStart=startDate;
            while (useStart.compareTo(endDate)<=0){
                EnergyTredResExtend energyTrendRes = new EnergyTredResExtend();
                energyTrendRes.setTime(DateUtil.dateToString(useStart,DateUtil.DATE_YEAR));
                useStart=DateUtil.stringToDate(DateUtil.addYear(useStart,1));
                result.add(energyTrendRes);
            }
        }
        return result;
    }



    @Operation(summary = "环比同比分析")
    @PostMapping("/yoyAndQoqAnalysis")
    @Authorize(ignore = true)
    public Flux<EnergyForecastTrendRes> trendForecast(@RequestBody EnergyTrendsReq req) {

        String year = req.getYear();
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.YEAR,Integer.valueOf(year));
        //开始时间
        instance.set(Calendar.DAY_OF_YEAR,instance.getActualMinimum(Calendar.DAY_OF_YEAR));
        Date startTimeE = instance.getTime();
        String start = DateUtil.dateToString(startTimeE, DateUtil.DATE_SHORT_FORMAT)+" 00:00:00";
        Date startTime = DateUtil.stringToDate(start, DateUtil.DATE_WITHSECOND_FORMAT);
        //结束时间
        instance.set(Calendar.DAY_OF_YEAR,instance.getActualMaximum(Calendar.DAY_OF_YEAR));
        Date endTimeE = instance.getTime();
        String end = DateUtil.dateToString(endTimeE, DateUtil.DATE_SHORT_FORMAT)+" 23:59:59";
        Date endTime = DateUtil.stringToDate(end, DateUtil.DATE_WITHSECOND_FORMAT);

        if(req.getDimension()==2){
            Map<String,EnergyForecastTrendRes> map=this.getResultListByDate(req.getDimension(), startTime, endTime);
            String sql="SELECT\n" +
                "   FROM_UNIXTIME((gather_time) / 1000, '%Y-%m') AS date,\n" +
                "    SUM(w.difference) AS number\n" +
                "FROM\n" +
                "    sems_"+req.getType()+"_consume w where w.device_id='0'\n" +
                "GROUP BY\n" +
                "    FROM_UNIXTIME((gather_time) / 1000, '%Y-%m');";

            return Flux.just(map)
                .flatMap(dataMap -> {
                    return queryHelper.select(sql, EnergyForecastTrendInfo::new)
                        .where(dsl -> dsl.lte("gather_time", endTime)
                            .gte("gather_time", startTime).noPaging())
                        .fetch()
                        .collectList()
                        .flatMapMany(currentInfos -> {
                            //3.填装map
                            if (currentInfos.size() > 0) {
                                BigDecimal currentMax=BigDecimal.ZERO;
                                for (EnergyForecastTrendInfo currentInfo : currentInfos) {
                                    if(currentInfo.getNumber().compareTo(currentMax)>0){
                                        currentMax=currentInfo.getNumber();
                                    }
                                    if (dataMap.get(currentInfo.getDate()) != null) {
                                        dataMap.get(currentInfo.getDate()).setCurrent(currentInfo.getNumber());
                                    }
                                }
                            }
                            //同比
                            long yoyStartDate = 0L;
                            long yoyEndDate = 0L;

                                yoyStartDate = DateUtil.stringToDate(DateUtil.addYears(startTime, -1), DateUtil.DATE_WITHSECOND_FORMAT).getTime();
                                yoyEndDate = DateUtil.stringToDate(DateUtil.addYears(endTime, -1), DateUtil.DATE_WITHSECOND_FORMAT).getTime();

                            //6.查询同比
                            long finalYoyEndDate = yoyEndDate;
                            long finalYoyStartDate = yoyStartDate;
                            return queryHelper.select(sql, EnergyForecastTrendInfo::new)
                                .where(dsl -> dsl.lte("gather_time", finalYoyEndDate)
                                    .gte("gather_time", finalYoyStartDate).noPaging())
                                .fetch()
                                .collectList()
                                .switchIfEmpty(Mono.just(new ArrayList<EnergyForecastTrendInfo>()))
                                .flatMapMany(yearOnYear -> {
                                    List<String> collect = yearOnYear.stream().map(EnergyForecastTrendInfo::getDate).collect(Collectors.toList());
                                    ArrayList<EnergyForecastTrendInfo> lists = new ArrayList<>();
                                    for (Map.Entry<String, EnergyForecastTrendRes> maps : map.entrySet()) {
                                        String key=DateUtil.addYear(DateUtil.stringToDate(maps.getKey(),DateUtil.DATE_SHORT_YEAR_MONTH),-1,DateUtil.DATE_SHORT_YEAR_MONTH);
                                        if(collect.contains(key)){
                                            lists.add(yearOnYear.stream().filter(i->i.getDate().equals(key)).findFirst().get());
                                        }else{
                                            EnergyForecastTrendInfo energyForecastTrendInfo = new EnergyForecastTrendInfo();
                                            energyForecastTrendInfo.setDate(key);
                                            energyForecastTrendInfo.setNumber(BigDecimal.ZERO);
                                            lists.add(energyForecastTrendInfo);
                                        }
                                    }

                                        for (EnergyForecastTrendInfo currentInfo : lists) {
                                            String date = currentInfo.getDate();
                                            String s = null;
                                            s = DateUtil.addYear(DateUtil.stringToDate(date, DateUtil.DATE_SHORT_YEAR_MONTH), 1,DateUtil.DATE_SHORT_YEAR_MONTH);
                                            if (dataMap.get(s) != null) {
                                                BigDecimal current=dataMap.get(s).getCurrent()==null?BigDecimal.ZERO:dataMap.get(s).getCurrent();
                                                dataMap.get(s).setYearOnYear(current.subtract(currentInfo.getNumber()));
                                            }
                                            }

                                        //环比
                                        long qoqStartDate = 0L;
                                        long qoqEndDate = 0L;

                                        qoqStartDate = DateUtil.stringToDate(DateUtil.addMonth(startTime, -1), DateUtil.DATE_WITHSECOND_FORMAT).getTime();
                                        qoqEndDate = DateUtil.stringToDate(DateUtil.addMonth(endTime, -1), DateUtil.DATE_WITHSECOND_FORMAT).getTime();
                                        //7.查询环比
                                        long finalQoqEndDate = qoqEndDate;
                                        long finalQoqStartDate = qoqStartDate;
                                        return queryHelper.select(sql, EnergyForecastTrendInfo::new)
                                            .where(dsl -> dsl.lte("gather_time", finalQoqEndDate)
                                                .gte("gather_time", finalQoqStartDate).noPaging())
                                            .fetch()
                                            .collectList()
                                            .switchIfEmpty(Mono.just(new ArrayList<EnergyForecastTrendInfo>()))
                                            .flatMapMany(monthOnMonth -> {
                                                if (monthOnMonth.size() > 0) {
                                                    EnergyForecastTrendInfo energyForecastTrendInfo = monthOnMonth.stream().min(Comparator.comparing(EnergyForecastTrendInfo::getDate)).orElseGet(EnergyForecastTrendInfo::new);
                                                    if(energyForecastTrendInfo.getDate()!= null){
                                                        String s = DateUtil.addMonth(DateUtil.stringToDate(energyForecastTrendInfo.getDate(), DateUtil.DATE_SHORT_YEAR_MONTH), -1, DateUtil.DATE_SHORT_YEAR_MONTH);
                                                        EnergyForecastTrendInfo energyForecastTrendInfoE = new EnergyForecastTrendInfo();
                                                        energyForecastTrendInfoE.setDate(s);
                                                        energyForecastTrendInfoE.setNumber(BigDecimal.ZERO);
                                                        monthOnMonth.add(energyForecastTrendInfoE);
                                                    }
                                                    for (EnergyForecastTrendInfo currentInfo : monthOnMonth) {
                                                        String date = currentInfo.getDate();
                                                        String s = null;
                                                        s = DateUtil.addMonth(DateUtil.stringToDate(date, DateUtil.DATE_SHORT_YEAR_MONTH), 1,DateUtil.DATE_SHORT_YEAR_MONTH);
                                                        if (dataMap.get(s) != null) {
                                                            BigDecimal current=dataMap.get(s).getCurrent()==null?BigDecimal.ZERO:dataMap.get(s).getCurrent();
                                                            dataMap.get(s).setMonthOnMonth(current.subtract(currentInfo.getNumber()));
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
                                                re.setYearOnYearRatio(getMonthOnMonth(re.getYearOnYear(), re.getCurrent(), 0));
                                                re.setMonthOnMontRatio(getMonthOnMonth(re.getMonthOnMonth(), re.getCurrent(), 1));
                                            }
                                            return Flux.fromIterable(res);
                                        });
                                });
                        });
                });
        }else {
            Map<String,EnergyForecastTrendRes> map=this.getResultListByDate(req.getDimension(), startTime,endTime);
            String sql="SELECT QUARTER(DATE_FORMAT(FROM_UNIXTIME((gather_time) / 1000), '%Y-%m-%d %H:%i:%S')) as " +
                "date,sum(difference) as number from sems_"+req.getType()+"_consume where device_id='0' group by date ";

            return Flux.just(map)
                .flatMap(dataMap -> {
                    return queryHelper.select(sql, EnergyForecastTrendInfo::new)
                        .where(dsl -> dsl.lte("gather_time", endTime)
                            .gte("gather_time", startTime).noPaging())
                        .fetch()
                        .collectList()
                        .flatMapMany(currentInfos -> {
                            Map<String, EnergyForecastTrendInfo> currentData= currentInfos.stream().collect(Collectors.toMap(EnergyForecastTrendInfo::getDate, Function.identity()));

                            //3.填装map
                            if (currentInfos.size() > 0) {


                                for (EnergyForecastTrendInfo currentInfo : currentInfos) {
                                    if (dataMap.get(currentInfo.getDate()) != null) {
                                        String date = currentInfo.getDate();
                                        if("1".equals(date)){
                                            dataMap.get(currentInfo.getDate()).setCurrent(currentInfo.getNumber());
                                            if(currentData.get("2")!= null){
                                                //2季度值
                                                BigDecimal number = currentData.get("2").getNumber();
                                                dataMap.get("2").setMonthOnMonth(number.subtract(currentInfo.getNumber()));
                                            }else {
                                                //2季度值
                                                BigDecimal number = BigDecimal.ZERO;
                                                dataMap.get("2").setMonthOnMonth(number.subtract(currentInfo.getNumber()));
                                            }

                                        }else if("2".equals(date)){
                                            //解决一年内季度不全季度，导致最开始的季度的环比为0的情况
                                            if(currentData.get("1")==null){
                                                dataMap.get("2").setMonthOnMonth(currentInfo.getNumber());
                                            }
                                            dataMap.get(currentInfo.getDate()).setCurrent(currentInfo.getNumber());
                                            if(currentData.get("2")!= null){
                                                //3季度值
                                                BigDecimal number = currentData.get("3").getNumber();
                                                dataMap.get("3").setMonthOnMonth(number.subtract(currentInfo.getNumber()));
                                            }else {
                                                //3季度值
                                                BigDecimal number = BigDecimal.ZERO;
                                                dataMap.get("3").setMonthOnMonth(number.subtract(currentInfo.getNumber()));
                                            }
                                        }else if("3".equals(date)){
                                            //解决一年内季度不全季度，导致最开始的季度的环比为0的情况
                                            if(currentData.get("2")==null){
                                                dataMap.get("3").setMonthOnMonth(currentInfo.getNumber());
                                            }
                                            dataMap.get(currentInfo.getDate()).setCurrent(currentInfo.getNumber());
                                            if(currentData.get("2")!= null){
                                                //4季度值
                                                BigDecimal number = currentData.get("4").getNumber();
                                                dataMap.get("4").setMonthOnMonth(number.subtract(currentInfo.getNumber()));
                                            }else {
                                                //4季度值
                                                BigDecimal number = BigDecimal.ZERO;
                                                dataMap.get("4").setMonthOnMonth(number.subtract(currentInfo.getNumber()));
                                            }
                                        }else {
                                            //解决一年内季度不全季度，导致最开始的季度的环比为0的情况
                                            if(currentData.get("3")==null){
                                                dataMap.get("4").setMonthOnMonth(currentInfo.getNumber());
                                            }
                                            dataMap.get(currentInfo.getDate()).setCurrent(currentInfo.getNumber());
                                        }
                                    }
                                }
                            }
                            //同比
                            long yoyStartDate = 0L;
                            long yoyEndDate = 0L;

                            yoyStartDate = DateUtil.stringToDate(DateUtil.addYears(startTime, -1), DateUtil.DATE_WITHSECOND_FORMAT).getTime();
                            yoyEndDate = DateUtil.stringToDate(DateUtil.addYears(endTime, -1), DateUtil.DATE_WITHSECOND_FORMAT).getTime();

                            //6.查询同比
                            long finalYoyEndDate = yoyEndDate;
                            long finalYoyStartDate = yoyStartDate;
                            return queryHelper.select(sql, EnergyForecastTrendInfo::new)
                                .where(dsl -> dsl.lte("gather_time", finalYoyEndDate)
                                    .gte("gather_time", finalYoyStartDate).noPaging())
                                .fetch()
                                .collectList()
                                .switchIfEmpty(Mono.just(new ArrayList<EnergyForecastTrendInfo>()))
                                .flatMapMany(yearOnYear -> {
                                    Map<String, EnergyForecastTrendInfo> collect = yearOnYear.stream().collect(Collectors.toMap(EnergyForecastTrendInfo::getDate, Function.identity()));
                                    ArrayList<EnergyForecastTrendInfo> newList = new ArrayList<>();
                                    if (yearOnYear.size() > 0) {
                                        for (int i = 1; i <=4; i++) {
                                            if(collect.get(String.valueOf(i))!= null){
                                                newList.add(collect.get(String.valueOf(i)));
                                            }else {
                                                EnergyForecastTrendInfo energyForecastTrendInfo = new EnergyForecastTrendInfo();
                                                energyForecastTrendInfo.setDate(String.valueOf(i));
                                                energyForecastTrendInfo.setNumber(BigDecimal.ZERO);
                                                newList.add(energyForecastTrendInfo);
                                            }
                                        }
                                        for (EnergyForecastTrendInfo currentInfo : newList) {
                                            String date = currentInfo.getDate();
                                            if (dataMap.get(date) != null) {
                                                BigDecimal current = dataMap.get(date).getCurrent();
                                                if("4".equals(date)){
                                                    BigDecimal currentNumber=dataMap.get("1").getCurrent()==null?BigDecimal.ZERO:dataMap.get("1").getCurrent();
                                                    dataMap.get("1").setMonthOnMonth(currentNumber.subtract(currentInfo.getNumber()));

                                                    dataMap.get(date).setYearOnYear(current.subtract(currentInfo.getNumber()));
                                                }else {
                                                    dataMap.get(date).setYearOnYear(current.subtract(currentInfo.getNumber()));
                                                }
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
                                                re.setYearOnYearRatio(getMonthOnMonth(re.getYearOnYear(), re.getCurrent(), 0));
                                                re.setMonthOnMontRatio(getMonthOnMonth(re.getMonthOnMonth(), re.getCurrent(), 1));
                                            }
                                            return Flux.fromIterable(res);
                                        });
                                });
                        });
        }
}

    public HashMap<String,EnergyForecastTrendRes> getResultListByDate(int dimension, Date start, Date end) {
        HashMap<String, EnergyForecastTrendRes> resultMap = new HashMap<>();

        if (dimension == 2) {
            //月
            Date startDate = start;

            while (end.compareTo(startDate)>=0) {
                EnergyForecastTrendRes energyForecastTrendRes = new EnergyForecastTrendRes();
                String date = DateUtil.dateToString(startDate, DateUtil.DATE_SHORT_YEAR_MONTH);
                energyForecastTrendRes.setDate(date);
                energyForecastTrendRes.setCurrent(BigDecimal.ZERO);
                energyForecastTrendRes.setYearOnYear(BigDecimal.ZERO);
                energyForecastTrendRes.setMonthOnMonth(BigDecimal.ZERO);
                resultMap.put(date,energyForecastTrendRes);
                startDate = DateUtil.addMonths(startDate, 1);
            }
            return resultMap;
        }
        if (dimension == 3) {
            //季
            List<String> seasonList = DateUtil.getSeasonList(start.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), end.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            for (String s : seasonList) {
                EnergyForecastTrendRes energyForecastTrendRes = new EnergyForecastTrendRes();;
                energyForecastTrendRes.setDate(s);
                energyForecastTrendRes.setCurrent(BigDecimal.ZERO);
                energyForecastTrendRes.setYearOnYear(BigDecimal.ZERO);
                energyForecastTrendRes.setMonthOnMonth(BigDecimal.ZERO);
                resultMap.put(s.substring(5,6),energyForecastTrendRes);
            }
            return resultMap;
        }
        return resultMap;

    }


    /**获得一个map的结果集
     *<pre>{@code
     *
     *}</pre>
     * @param
     * @return
     * @see
     */
    public static void getMap(long startTime, long endTime, Map<String,EnergyForecastTrendRes> map,String format){

        LocalDateTime startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.of("Asia/Shanghai"));
        LocalDateTime endDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.of("Asia/Shanghai"));
        List<String> list = DateUtil.getHoursBetweenDates(format, startDateTime, endDateTime);
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
        public static BigDecimal getMonthOnMonth(BigDecimal last,BigDecimal now,int flag){
            if(last==null){
                last=BigDecimal.ZERO;
            }
            if(now==null){
                now=BigDecimal.ZERO;
            }
            //上一期
            BigDecimal temp = now.subtract(last);
            if(temp.compareTo(BigDecimal.ZERO)==0){
                if(now.compareTo(BigDecimal.ZERO)==0) return BigDecimal.ZERO;
                else if(flag==0) return BigDecimal.valueOf(100); else return BigDecimal.ZERO ;
            }else {
                return last.divide(temp,BigDecimal.ROUND_CEILING).multiply(BigDecimal.valueOf(100));
            }
        }


        @Operation(summary = "能耗看板报告")
        @PostMapping("/test")
        @Authorize(ignore = true)
        public Mono<Map> getReport(@RequestBody EnergyTrendsReq energyTrendsReq){
            BoardReportRes boardReportRes = new BoardReportRes();
            BoardPicReportRes boardPicReportRes = new BoardPicReportRes();
            //如果时间间隔为一条，只展示日期
            int days = DateUtil.daysOfTwo(energyTrendsReq.getStartDate(), energyTrendsReq.getEndDate())+1;
            if(days<=1){
                boardReportRes.setTime(DateUtil.dateToString(energyTrendsReq.getStartDate(),DateUtil.DATE_SHORT_FORMAT));
            }else {
                boardReportRes.setTime(DateUtil.dateToString(energyTrendsReq.getStartDate(),DateUtil.DATE_SHORT_FORMAT)+"至"+DateUtil.dateToString(energyTrendsReq.getEndDate(),DateUtil.DATE_SHORT_FORMAT));
            }

            boardPicReportRes.setTime(DateUtil.dateToString(energyTrendsReq.getStartDate(),DateUtil.DATE_SHORT_FORMAT)+"至"+DateUtil.dateToString(energyTrendsReq.getEndDate(),DateUtil.DATE_SHORT_FORMAT));
            String type = energyTrendsReq.getType();

            HashMap<String, Object> map = new HashMap<>();

            if("electricity".equals(energyTrendsReq.getType())){
                Long[] dates={energyTrendsReq.getStartDate().getTime(),energyTrendsReq.getEndDate().getTime()};
                //1.总能耗
                String sql="select  sum(difference) as value,sum(difference*unit_price) as cost,periods_type as type  from sems_electricity_consume where device_id='0' GROUP BY  periods_type";
                return queryHelper
                    .select(sql,EnergyTrendRes::new)
                    .where(dsl->dsl.and("gather_time","btw",dates))
                    .fetch()
                    .collectList()
                    .flatMap(va->{
                        //总用能
                        BigDecimal totalCost = va.stream().filter(v->v.getCost() != null).map(EnergyTrendRes::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal total = va.stream().filter(v->v.getValue() != null).map(EnergyTrendRes::getValue).reduce(BigDecimal.ZERO,BigDecimal::add);
                        boardReportRes.setTotalUse(total.setScale(2,RoundingMode.HALF_UP));
                        boardReportRes.setTotalCost(totalCost.setScale(2,RoundingMode.HALF_UP));
                        boardPicReportRes.setTotalUse(total);
                        //判断时间范围
                        int i = DateUtil.daysOfTwo(energyTrendsReq.getStartDate(),energyTrendsReq.getEndDate());
                        if(i<32 && i>=1){
                            BigDecimal divide = total.divide(BigDecimal.valueOf(i),2,BigDecimal.ROUND_HALF_UP);
                            boardReportRes.setDayAvgUse(divide);
                        }else if(i>=32){
                            //判断有多少个月
                            BigDecimal divide = BigDecimal.valueOf(i).divide(BigDecimal.valueOf(31), 0);
                            BigDecimal divide1 = total.divide(divide, 2, BigDecimal.ROUND_HALF_UP);
                            boardReportRes.setMonthAvgUse(divide1);
                        }else {
                            boardReportRes.setDayAvgUse(total);
                        }

                        Expression carboneMissionFormula;
                        Expression standardCoalFormula;
                        //1. 根据不同的类型选择不同的计算公式
                        if(type.equals("water")) {
                            carboneMissionFormula = calculationService.getCarboneMissionFormula(1);
                            standardCoalFormula = calculationService.getStandardCoalFormula(1);
                        }else if(type.equals("electricity")){
                            carboneMissionFormula = calculationService.getCarboneMissionFormula(2);
                            standardCoalFormula = calculationService.getStandardCoalFormula(2);
                        }else {
                            carboneMissionFormula = calculationService.getCarboneMissionFormula(3);
                            standardCoalFormula = calculationService.getStandardCoalFormula(3);
                        }
                        BigDecimal calculate = calculationService.Calculate(total, carboneMissionFormula);
                        BigDecimal coal = calculationService.Calculate(total, standardCoalFormula);

                        boardReportRes.setCarBon(calculate.setScale(2,RoundingMode.HALF_UP));
                        boardReportRes.setCoal(coal.setScale(2,RoundingMode.HALF_UP));
                        boardReportRes.set_1user(BigDecimal.ZERO);
                        boardReportRes.set_1cost(BigDecimal.ZERO);
                        boardReportRes.set_2user(BigDecimal.ZERO);
                        boardReportRes.set_2cost(BigDecimal.ZERO);
                        boardReportRes.set_3user(BigDecimal.ZERO);
                        boardReportRes.set_3cost(BigDecimal.ZERO);
                        boardReportRes.set_4user(BigDecimal.ZERO);
                        boardReportRes.set_4cost(BigDecimal.ZERO);

                        for (EnergyTrendRes energyTrendRes : va) {
                            if("1".equals(energyTrendRes.getType())){
                                boardReportRes.set_1user(energyTrendRes.getValue()==null?BigDecimal.ZERO:energyTrendRes.getValue().setScale(2,RoundingMode.HALF_UP));
                                boardReportRes.set_1cost(energyTrendRes.getCost()==null?BigDecimal.ZERO:energyTrendRes.getCost().setScale(2,RoundingMode.HALF_UP));
                            }else if("2".equals(energyTrendRes.getType())){
                                boardReportRes.set_2user(energyTrendRes.getValue()==null?BigDecimal.ZERO:energyTrendRes.getValue().setScale(2,RoundingMode.HALF_UP));
                                boardReportRes.set_2cost(energyTrendRes.getCost()==null?BigDecimal.ZERO:energyTrendRes.getCost().setScale(2,RoundingMode.HALF_UP));
                            }else if("3".equals(energyTrendRes.getType())){
                                boardReportRes.set_3user(energyTrendRes.getValue()==null?BigDecimal.ZERO:energyTrendRes.getValue().setScale(2,RoundingMode.HALF_UP));
                                boardReportRes.set_3cost(energyTrendRes.getCost()==null?BigDecimal.ZERO:energyTrendRes.getCost().setScale(2,RoundingMode.HALF_UP));
                            }else {
                                boardReportRes.set_4user(energyTrendRes.getValue()==null?BigDecimal.ZERO:energyTrendRes.getValue().setScale(2,RoundingMode.HALF_UP));
                                boardReportRes.set_4cost(energyTrendRes.getCost()==null?BigDecimal.ZERO:energyTrendRes.getCost().setScale(2,RoundingMode.HALF_UP));
                            }
                        }
                        map.put("last",boardReportRes);
                        return Mono.just(map);
                    });
        }else{
                    Long[] dates={energyTrendsReq.getStartDate().getTime(),energyTrendsReq.getEndDate().getTime()};
                    //1.总能耗
                    String sql="select  sum(difference) as value ,sum(difference*unit_price) as  cost from sems_"+energyTrendsReq.getType()+"_consume where device_id='0' ";
                    return queryHelper
                        .select(sql,EnergyTrendRes::new)
                        .where(dsl->dsl.and("gather_time","btw",dates))
                        .fetch()
                        .collectList()
                        .flatMap(va->{
                            //总用能
                            BigDecimal totalCost = va.stream().filter(c->c.getCost()!=null).map(EnergyTrendRes::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
                            BigDecimal total = va.stream().filter(c->c.getValue()!= null).map(EnergyTrendRes::getValue).reduce(BigDecimal.ZERO,BigDecimal::add);
                            boardReportRes.setTotalUse(total.setScale(2,RoundingMode.HALF_UP));
                            boardReportRes.setTotalCost(totalCost.setScale(2,RoundingMode.HALF_UP));
                            //判断时间范围
                            int i = DateUtil.daysOfTwo(energyTrendsReq.getStartDate(),energyTrendsReq.getEndDate());
                            if(i<32 && i>=1){
                                BigDecimal divide = total.divide(BigDecimal.valueOf(i),2,BigDecimal.ROUND_HALF_UP);
                                boardReportRes.setDayAvgUse(divide);
                            }else if(i>=32){
                                //判断有多少个月
                                BigDecimal divide = BigDecimal.valueOf(i).divide(BigDecimal.valueOf(31), 0);
                                BigDecimal divide1 = total.divide(divide, 2, BigDecimal.ROUND_HALF_UP);
                                boardReportRes.setMonthAvgUse(divide1);
                            }else {
                                boardReportRes.setDayAvgUse(total.setScale(2,RoundingMode.HALF_UP));
                            }

                            Expression carboneMissionFormula;
                            Expression standardCoalFormula;
                            //1. 根据不同的类型选择不同的计算公式
                            if(type.equals("water")) {
                                carboneMissionFormula = calculationService.getCarboneMissionFormula(1);
                                standardCoalFormula = calculationService.getStandardCoalFormula(1);
                            }else if(type.equals("electricity")){
                                carboneMissionFormula = calculationService.getCarboneMissionFormula(2);
                                standardCoalFormula = calculationService.getStandardCoalFormula(2);
                            }else {
                                carboneMissionFormula = calculationService.getCarboneMissionFormula(3);
                                standardCoalFormula = calculationService.getStandardCoalFormula(3);
                            }
                            BigDecimal calculate = calculationService.Calculate(total, carboneMissionFormula);
                            BigDecimal coal = calculationService.Calculate(total, standardCoalFormula);

                            boardReportRes.setCarBon(calculate.setScale(2,RoundingMode.HALF_UP));
                            boardReportRes.setCoal(coal.setScale(2,RoundingMode.HALF_UP));
                            map.put("last",boardReportRes);
                            return Mono.just(map);

                });

                }
        }




        @Operation(summary = "能耗看板报告下面的")
        @PostMapping("/underReport")
        @Authorize(ignore = true)
        public Mono getUnderReport(@RequestBody EnergyTrendsReq energyTrendsReq) {
            BoardReportRes boardReportRes = new BoardReportRes();
            BoardPicReportRes boardPicReportRes = new BoardPicReportRes();
            boardReportRes.setTime(DateUtil.dateToString(energyTrendsReq.getStartDate(), DateUtil.DATE_SHORT_FORMAT) + "至" + DateUtil.dateToString(energyTrendsReq.getEndDate(), DateUtil.DATE_SHORT_FORMAT));
            int days = DateUtil.daysOfTwo(energyTrendsReq.getStartDate(), energyTrendsReq.getEndDate()) + 1;
            if (days <= 1) {
                boardPicReportRes.setTime(DateUtil.dateToString(energyTrendsReq.getStartDate(), DateUtil.DATE_SHORT_FORMAT));
            } else {
                boardPicReportRes.setTime(DateUtil.dateToString(energyTrendsReq.getStartDate(), DateUtil.DATE_SHORT_FORMAT) + "至" + DateUtil.dateToString(energyTrendsReq.getEndDate(), DateUtil.DATE_SHORT_FORMAT));
            }


            HashMap<String, Object> map = new HashMap<>();

            return this.getMap(energyTrendsReq)
                .flatMap(va -> {
                    BigDecimal currenTotal = (BigDecimal) va.get("currenTotal");
                    BigDecimal yoyTotal = (BigDecimal) va.get("yoyTotal");
                    BigDecimal qoqTotal = (BigDecimal) va.get("qoqTotal");
                    //总的
                    boardPicReportRes.setTotalUse(currenTotal.setScale(2, RoundingMode.HALF_UP));

                    //同比
                    if (yoyTotal.compareTo(BigDecimal.ZERO) == 0) {
                        if(currenTotal.compareTo(BigDecimal.ZERO)==0){
                            boardPicReportRes.setYoy(BigDecimal.valueOf(0));
                        }else {
                            boardPicReportRes.setYoy(BigDecimal.valueOf(100));
                        }
                        boardPicReportRes.setHistoryUse(yoyTotal.setScale(2, RoundingMode.HALF_UP));
                    } else {
                        BigDecimal divide = currenTotal.subtract(yoyTotal).divide(yoyTotal, 2, BigDecimal.ROUND_HALF_UP);
                        boardPicReportRes.setHistoryUse(yoyTotal);
                        boardPicReportRes.setYoy(divide.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
                    }
                    //环比

                    if (qoqTotal.compareTo(BigDecimal.ZERO) == 0) {
                        boardPicReportRes.setLastUse(qoqTotal);
                        boardPicReportRes.setQoq(BigDecimal.ZERO);
                    } else {
                        BigDecimal divide = currenTotal.subtract(qoqTotal).divide(qoqTotal, 2, BigDecimal.ROUND_HALF_UP);
                        boardPicReportRes.setLastUse(qoqTotal);
                        boardPicReportRes.setQoq(divide.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
                    }
                    if (va.get("time") == null) {
                        return Mono.empty();
                    }
                    boardPicReportRes.setTime2(va.get("time").toString());
                    BigDecimal current = (BigDecimal) va.get("current");
                    BigDecimal yoyValue = (BigDecimal) va.get("yoy");
                    BigDecimal qoqValue = (BigDecimal) va.get("qoq");
                    if (current.compareTo(yoyValue) > 0) {
                        boardPicReportRes.setTag("高于历史同期");
                        if (yoyValue.compareTo(BigDecimal.ZERO) == 0) {
                            boardPicReportRes.setRat(current.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
                        } else {
                            BigDecimal divide = current.subtract(yoyValue).divide(yoyValue, 2, BigDecimal.ROUND_HALF_UP);
                            boardPicReportRes.setRat(divide.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
                        }
                    } else {
                        boardPicReportRes.setTag("低于历史同期");
                        if (yoyValue.compareTo(BigDecimal.ZERO) == 0) {
                            boardPicReportRes.setRat(current.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
                        } else {
                            BigDecimal divide = current.subtract(yoyValue).abs().divide(yoyValue, 2, BigDecimal.ROUND_HALF_UP);
                            boardPicReportRes.setRat(divide.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
                        }
                    }

                    if (current.compareTo(qoqValue) > 0) {
                        boardPicReportRes.setTag1("高于上一周期");
                        if (qoqValue.compareTo(BigDecimal.ZERO) == 0) {
                            boardPicReportRes.setRat1(current.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
                        } else {
                            BigDecimal divide = current.subtract(qoqValue).divide(qoqValue, 2, BigDecimal.ROUND_HALF_UP);
                            boardPicReportRes.setRat1(divide.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
                        }
                    } else {
                        boardPicReportRes.setTag1("低于上一周期");
                        if (qoqValue.compareTo(BigDecimal.ZERO) == 0) {
                            boardPicReportRes.setRat1(current.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
                        } else {
                            BigDecimal divide = current.subtract(qoqValue).abs().divide(qoqValue, 2, BigDecimal.ROUND_HALF_UP);
                            boardPicReportRes.setRat1(divide.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
                        }
                    }
                    map.put("under", boardPicReportRes);
                    return Mono.just(map);

                });
        }

    /**
     * 获取当前最大，时间段同比
     * @param req
     * @return
     */
    public Mono<Map<String,Object>> getMap(EnergyTrendsReq req) {
        //当前最大
        HashMap<String,Object> resultMap = new HashMap<>();


        Map<String,EnergyForecastTrendRes> map = new HashMap<>();
        getMap(req.getStartDate().getTime(),req.getEndDate().getTime(),map,"%Y-%m");
        String sql="SELECT\n" +
            "   FROM_UNIXTIME((gather_time) / 1000, '%Y-%m') AS date,\n" +
            "    SUM(w.difference) AS number\n" +
            "FROM\n" +
            "    sems_"+req.getType()+"_consume w where w.device_id='0'\n" +
            "GROUP BY\n" +
            "    FROM_UNIXTIME((gather_time) / 1000, '%Y-%m');";

                return queryHelper.select(sql, EnergyForecastTrendInfo::new)
                    .where(dsl -> dsl.lte("gather_time", req.getEndDate())
                        .gte("gather_time", req.getStartDate()).noPaging())
                    .fetch()
                    .collectList()
                    .flatMap(currentInfos -> {
                        //3.填装map
                        BigDecimal currentMax=BigDecimal.ZERO;
                        String time=null;
                        BigDecimal currentTotalNum=BigDecimal.ZERO;
                        if (currentInfos.size() > 0) {
                            for (EnergyForecastTrendInfo currentInfo : currentInfos) {
                                if(currentInfo.getNumber().compareTo(currentMax)>0){
                                    currentMax=currentInfo.getNumber();
                                    time=currentInfo.getDate();
                                }
                                currentTotalNum=currentTotalNum.add(currentInfo.getNumber());                            }
                        }
                        if(time==null){
                            time=DateUtil.dateToString(req.getStartDate(),DateUtil.DATE_SHORT_YEAR_MONTH);
                        }
                        resultMap.put("current",currentMax);
                        resultMap.put("time",time);
                        resultMap.put("currenTotal",currentTotalNum);

                        //同比日期
                        String lastYearTime = DateUtil.addYear(DateUtil.stringToDate(time, DateUtil.DATE_SHORT_YEAR_MONTH),-1,DateUtil.DATE_SHORT_YEAR_MONTH);
                        //环比
                        String lastTime = DateUtil.addMonth(DateUtil.stringToDate(time, DateUtil.DATE_SHORT_YEAR_MONTH),-1,DateUtil.DATE_SHORT_YEAR_MONTH);
                        //同比

                            Long yoyStartDate = DateUtil.stringToDate(DateUtil.addYears(req.getStartDate(), -1), DateUtil.DATE_WITHSECOND_FORMAT).getTime();
                            Long yoyEndDate = DateUtil.stringToDate(DateUtil.addYears(req.getEndDate(), -1), DateUtil.DATE_WITHSECOND_FORMAT).getTime();

                        //6.查询同比

                        long finalYoyEndDate = yoyEndDate;
                        long finalYoyStartDate = yoyStartDate;
                        return queryHelper.select(sql, EnergyForecastTrendInfo::new)
                            .where(dsl -> dsl.lte("gather_time", finalYoyEndDate)
                                .gte("gather_time", finalYoyStartDate).noPaging())
                            .fetch()
                            .collectList()
                            .switchIfEmpty(Mono.just(new ArrayList<EnergyForecastTrendInfo>()))
                            .flatMap(yearOnYear -> {
                               BigDecimal yoyMax=BigDecimal.ZERO;
                               BigDecimal yoyTotal=BigDecimal.ZERO;

                                if (yearOnYear.size() > 0) {
                                    yoyMax = BigDecimal.ZERO;

                                    for (EnergyForecastTrendInfo currentInfo : yearOnYear) {
                                        if (currentInfo.getDate().equals(lastYearTime)) {
                                            yoyMax = currentInfo.getNumber();
                                        }
                                        yoyTotal=yoyTotal.add(currentInfo.getNumber());
                                    }
                                }
                                resultMap.put("yoy",yoyMax);
                                resultMap.put("yoyTotal",yoyTotal);

                                //环比

                                    long qoqStartDate = DateUtil.stringToDate(DateUtil.addMonth(req.getStartDate(), -1), DateUtil.DATE_WITHSECOND_FORMAT).getTime();

                                Date date=DateUtil.getLastDateOfMonth(DateUtil.addMonths(req.getEndDate(), -1));
                                String dateTime=DateUtil.dateToString(date,DateUtil.DATE_SHORT_FORMAT)+ " 23:59:59";

                                long qoqEndDate = DateUtil.stringToDate(dateTime, DateUtil.DATE_WITHSECOND_FORMAT).getTime();
                                //7.查询环比

                                long finalQoqEndDate = qoqEndDate;
                                long finalQoqStartDate = qoqStartDate;
                                return queryHelper.select(sql, EnergyForecastTrendInfo::new)
                                    .where(dsl -> dsl.lte("gather_time", finalQoqEndDate)
                                        .gte("gather_time", finalQoqStartDate).noPaging())
                                    .fetch()
                                    .collectList()
                                    .switchIfEmpty(Mono.just(new ArrayList<EnergyForecastTrendInfo>()))
                                    .flatMap(monthOnMonth -> {
                                        BigDecimal qoqMax=BigDecimal.ZERO;
                                        BigDecimal qoqTotal=BigDecimal.ZERO;
                                        if (monthOnMonth.size() > 0) {
                                            qoqMax=BigDecimal.ZERO;
                                            for (EnergyForecastTrendInfo currentInfo : monthOnMonth) {
                                                if (currentInfo.getDate().equals(lastTime)) {
                                                    qoqMax=currentInfo.getNumber();
                                                }
                                                qoqTotal=qoqTotal.add(currentInfo.getNumber());
                                            }

                                        }
                                        resultMap.put("qoq",qoqMax);
                                        resultMap.put("qoqTotal",qoqTotal);

                                        return Mono.just(resultMap);
                                    });
                            });
                    });

    }



}


