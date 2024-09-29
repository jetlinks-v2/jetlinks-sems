package org.jetlinks.pro.sems.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.sems.entity.req.EnergyReportReq;
import org.jetlinks.pro.sems.entity.res.EnergyDayReportRes;
import org.jetlinks.pro.sems.entity.res.EnergyDayRes;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.jetlinks.pro.sems.service.AreaInfoService;
import org.jetlinks.pro.sems.service.DeviceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/sems/energy/report")
@AllArgsConstructor
@Getter
@Tag(name = "能源报表1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "energy-report", name = "能源报表")
public class EnergyReportController {

    private final DeviceService deviceService;

    private final AreaInfoService areaInfoService;

    private final QueryHelper queryHelper;

    @Operation(summary = "能耗月报")
    @PostMapping("/_energy/_day/_list")
    @QueryAction
    public Flux<EnergyDayReportRes> getEnergyDayList(@RequestBody EnergyReportReq req) {
        List<String> energyList = new ArrayList<>();
        if(Objects.isNull(req.getEnergyType())){
            energyList.add("water");
            energyList.add("electricity");
            energyList.add("gas");
        } else {
            energyList.add(req.getEnergyType().getValue());
        }
        return Flux.fromIterable(energyList)
                .flatMap(name -> queryHelper
                    .select("SELECT \n" +
                                "DAY(FROM_UNIXTIME(ROUND(gather_time/1000,0))) as gatherTime,\n" +
                                "SUM(difference) as difference, \n" +
                                "sum(difference*unit_price) as cost \n"+
                                "FROM sems_" + name + "_consume where device_id='0'\n" +
                                "GROUP BY DAY(FROM_UNIXTIME(ROUND(gather_time/1000,0)))\n" +
                                "ORDER BY DAY(FROM_UNIXTIME(ROUND(gather_time/1000,0)))", EnergyDayRes::new)
                    .where(dsl -> dsl.and("gather_time", "btw", req.getGatherTime())
                                     .notNull("difference"))
                    .fetch()
                    .collectList()
                    .flatMap(list -> {
                        LocalDate gatherDate = Instant.ofEpochMilli(req.getGatherTime()[0])
                                                      .atZone(ZoneId.systemDefault())
                                                      .toLocalDate();
                        LocalDate localDate = LocalDate.now();
                        int monthDay;
                        if(gatherDate.getMonthValue() == localDate.getMonthValue()){
                            monthDay = localDate.getDayOfMonth();
                        }else {
                            monthDay = gatherDate.lengthOfMonth();
                        }
                        BigDecimal energySum = list
                            .stream()
                            .map(EnergyDayRes::getDifference)
                            .collect(Collectors.toList())
                            .stream()
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                        //总费用
                        BigDecimal totalCost = list
                            .stream()
                            .map(EnergyDayRes::getCost)
                            .collect(Collectors.toList())
                            .stream()
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);
                        EnergyDayReportRes res = new EnergyDayReportRes();
                        res.setEnergyDayResList(list);
                        res.setEnergy(name);
                        res.setMonthAvgEnergy(energySum.divide(new BigDecimal(monthDay),2,BigDecimal.ROUND_HALF_UP));
                        res.setMonthSumEnergy(energySum);
                        res.setTotalCost(totalCost);

                        return getYoyQoQ(res,gatherDate);
                    }));
    }


    private Mono<EnergyDayReportRes> getYoyQoQ(EnergyDayReportRes res,LocalDate gatherDate){
        //同比日期--比去年的月份
        LocalDate yoyDate = gatherDate.minusYears(1);
        //环比日期--比上个月
        LocalDate qoqDate = gatherDate.minusMonths(1);

        //环比对应月份开始时间
        long qoqYearStart = LocalDateTime.of(LocalDate.of(qoqDate.getYear(),qoqDate.getMonthValue(),1), LocalTime.MIN)
                                          .toInstant(ZoneOffset.of("+8"))
                                          .toEpochMilli();
        //环比对应月份结束时间
        long qoqYearEnd = LocalDateTime.of(LocalDate.of(qoqDate.getYear(),qoqDate.getMonthValue(),qoqDate.lengthOfMonth()), LocalTime.MAX)
                                        .toInstant(ZoneOffset.of("+8"))
                                        .toEpochMilli();

        //同比对应月份开始时间
        long yoyYearStart = LocalDateTime.of(LocalDate.of(yoyDate.getYear(),yoyDate.getMonthValue(),1), LocalTime.MIN)
                                          .toInstant(ZoneOffset.of("+8"))
                                          .toEpochMilli();
        //同比应月份结束时间
        long yoyYearEnd = LocalDateTime.of(LocalDate.of(yoyDate.getYear(),yoyDate.getMonthValue(),yoyDate.lengthOfMonth()), LocalTime.MAX)
                                        .toInstant(ZoneOffset.of("+8"))
                                        .toEpochMilli();
        return queryHelper
            .select("SELECT \n" +
                        "IFNULL(SUM(difference),0) as difference \n" +
                        "FROM sems_" + res.getEnergy() + "_consume \n" +
                        "WHERE `gather_time` between ? and ? and device_id='0' ", EnergyDayRes::new,qoqYearStart,qoqYearEnd)
            .where(dsl -> dsl.notNull("difference"))
            .fetch()
            .flatMap(energyDayRes -> {
                BigDecimal lastSumDifference = energyDayRes.getDifference();
                BigDecimal qoqValue = BigDecimal.valueOf(100);
                if(lastSumDifference.compareTo(BigDecimal.ZERO) > 0 ){
                    qoqValue = (res.getMonthSumEnergy()
                                          .subtract(lastSumDifference))
                    .divide(lastSumDifference,2)
                    .multiply(BigDecimal.valueOf(100));
                }
                if(lastSumDifference.compareTo(BigDecimal.ZERO) == 0 ){
                    qoqValue = BigDecimal.ZERO;
                }

                res.setQoq(qoqValue);
                return Mono.just(res);
            })
            .flatMap(energyDayReportRes -> queryHelper
                .select("SELECT \n" +
                            "IFNULL(SUM(difference),0) as difference \n" +
                            "FROM sems_" + res.getEnergy() + "_consume \n" +
                            "WHERE `gather_time` between ? and ? and device_id='0' ", EnergyDayRes::new,yoyYearStart,yoyYearEnd)
                .where(dsl -> dsl.notNull("difference"))
                .fetch()
                .flatMap(energyDayRes -> {
                    BigDecimal thisSumDifference = energyDayRes.getDifference();
                    BigDecimal yoyValue = BigDecimal.ZERO;
                    if(thisSumDifference.compareTo(BigDecimal.ZERO) > 0 ){
                        yoyValue = (res.getMonthSumEnergy()
                                                  .subtract(thisSumDifference))
                            .divide(thisSumDifference,2)
                            .multiply(BigDecimal.valueOf(100));
                    }

                    if(thisSumDifference.compareTo(BigDecimal.ZERO) == 0 ){
                        yoyValue = BigDecimal.ZERO;
                    }

                    res.setYoy(yoyValue);
                    return Mono.just(res);
                }))
            .collectList()
            .flatMap(list -> Mono.just(list.get(0)));

    }

}
