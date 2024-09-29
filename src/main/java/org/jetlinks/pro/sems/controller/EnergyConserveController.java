package org.jetlinks.pro.sems.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.sems.entity.req.EnergyConserveReq;
import org.jetlinks.pro.sems.entity.res.EnergyConserveRes;
import org.jetlinks.pro.sems.entity.res.EnergyDayRes;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/energy/conserve")
@AllArgsConstructor
@Getter
@Tag(name = "节能效益1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "energy-conserve", name = "节能效益")
public class EnergyConserveController {

    private final QueryHelper queryHelper;

    @Operation(summary = "用电节能分析")
    @PostMapping("/electricity/conserve/analysis")
    @Authorize(ignore = true)
    public Mono<Object> getEnergyDayList(@RequestBody EnergyConserveReq req) {

        List<EnergyConserveRes> energyConserveResList = new ArrayList<>();

        return queryHelper
            .select("SELECT\n" +
                        "DATE_FORMAT(Date(FROM_UNIXTIME(ROUND(gather_time/1000,0))),\"%Y-%m\") as gatherTime,\n" +
                        "IFNULL(SUM(difference),0) as difference\n" +
                        "FROM sems_electricity_consume where device_id='0'\n" +
                        "GROUP BY DATE_FORMAT(Date(FROM_UNIXTIME(ROUND(gather_time/1000,0))),\"%Y-%m\")\n" +
                        "ORDER BY DATE_FORMAT(Date(FROM_UNIXTIME(ROUND(gather_time/1000,0))),\"%Y-%m\")", EnergyDayRes::new)
            .where(dsl -> dsl.and("gather_time", "btw", req.getConserveStartTime())
                             .notNull("difference"))
            .fetch()
            .collectMap(EnergyDayRes::getGatherTime, EnergyDayRes::getDifference)
            .flatMap(dayResMap ->{
                List<EnergyDayRes> dayResList = new ArrayList<>();
                YearMonth start = YearMonth.from(Instant.ofEpochSecond(req.getConserveStartTime()[0]/1000).atZone(ZoneId.systemDefault()));
                YearMonth end = YearMonth.from(Instant.ofEpochSecond(req.getConserveStartTime()[1]/1000).atZone(ZoneId.systemDefault()));
                EnergyConserveRes res = new EnergyConserveRes();
                do {
                    EnergyDayRes dayRes = new EnergyDayRes();
                    dayRes.setGatherTime(start.toString());
                    dayRes.setDifference(BigDecimal.ZERO);
                    dayResList.add(dayRes);
                    start = start.plusMonths(1);
                } while (start.compareTo(end) <= 0);
                res.setName("节能前");
                for (EnergyDayRes dayRes:dayResList) {
                    if(dayResMap.containsKey(dayRes.getGatherTime())){
                        dayRes.setDifference(dayResMap.get(dayRes.getGatherTime()));
                    }
                }
                BigDecimal energySum = dayResList
                    .stream()
                    .map(EnergyDayRes::getDifference)
                    .collect(Collectors.toList())
                    .stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                res.setSumEnergy(energySum);
                res.setEnergyList(dayResList);
                energyConserveResList.add(res);
                return Mono.just(energyConserveResList);

            })
            .flatMap(resList ->{
                return queryHelper
                    .select("SELECT\n" +
                                "DATE_FORMAT(Date(FROM_UNIXTIME(ROUND(gather_time/1000,0))),\"%Y-%m\") as gatherTime,\n" +
                                "IFNULL(SUM(difference),0) as difference\n" +
                                "FROM sems_electricity_consume where device_id='0'\n" +
                                "GROUP BY DATE_FORMAT(Date(FROM_UNIXTIME(ROUND(gather_time/1000,0))),\"%Y-%m\")\n" +
                                "ORDER BY DATE_FORMAT(Date(FROM_UNIXTIME(ROUND(gather_time/1000,0))),\"%Y-%m\")", EnergyDayRes::new)
                    .where(dsl -> dsl.and("gather_time", "btw", req.getConserveEndTime())
                                     .notNull("difference"))
                    .fetch()
                    .collectMap(EnergyDayRes::getGatherTime, EnergyDayRes::getDifference)
                    .flatMap(dayResMap ->{
                        List<EnergyDayRes> dayResList = new ArrayList<>();
                        YearMonth start = YearMonth.from(Instant.ofEpochSecond(req.getConserveEndTime()[0]/1000).atZone(ZoneId.systemDefault()));
                        YearMonth end = YearMonth.from(Instant.ofEpochSecond(req.getConserveEndTime()[1]/1000).atZone(ZoneId.systemDefault()));
                        EnergyConserveRes res = new EnergyConserveRes();
                        do {
                            EnergyDayRes dayRes = new EnergyDayRes();
                            dayRes.setGatherTime(start.toString());
                            dayRes.setDifference(BigDecimal.ZERO);
                            dayResList.add(dayRes);
                            start = start.plusMonths(1);
                        } while (start.compareTo(end) <= 0);
                        res.setName("节能后");
                        for (EnergyDayRes dayRes:dayResList) {
                            if(dayResMap.containsKey(dayRes.getGatherTime())){
                                dayRes.setDifference(dayResMap.get(dayRes.getGatherTime()));
                            }
                        }
                        BigDecimal energySum = dayResList
                            .stream()
                            .map(EnergyDayRes::getDifference)
                            .collect(Collectors.toList())
                            .stream()
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        res.setSumEnergy(energySum);
                        res.setEnergyList(dayResList);
                        energyConserveResList.add(res);
                        return Mono.just(energyConserveResList);
                    });
            })
            .flatMap(list ->{
                JSONObject resObject = new JSONObject();
                resObject.put("list",list);
                BigDecimal percentage = BigDecimal.valueOf(0);
                if(list.get(0).getSumEnergy().compareTo(BigDecimal.ZERO) != 0){
                    percentage = list.get(1).getSumEnergy()
                                     .subtract(list.get(0).getSumEnergy())
                                     .divide(list.get(0).getSumEnergy(),2)
                                     .multiply(BigDecimal.valueOf(100));
                }

                resObject.put("percentage",percentage);
                return Mono.just(resObject);
            });

    }
}
