package org.jetlinks.pro.sems.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.sems.entity.req.EnergyEfficiencyReq;
import org.jetlinks.pro.sems.entity.res.EfficiencyRes;
import org.jetlinks.pro.sems.entity.res.EnergyEffectiveReportRes;
import org.jetlinks.pro.sems.entity.res.EnergyEfficiencyRes;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.jetlinks.pro.sems.entity.TestEnergyDetailEntity;
import org.jetlinks.pro.sems.entity.TestRecordEntity;
import org.jetlinks.pro.sems.service.ElectricityConsumeService;
import org.jetlinks.pro.sems.service.TestConfigDeviceService;
import org.jetlinks.pro.sems.service.TestEnergyDetailService;
import org.jetlinks.pro.sems.service.TestRecordService;
import org.jetlinks.pro.sems.utils.DateUtil;
import org.joda.time.DateTime;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;

@RestController
@RequestMapping("/sems/energy/efficiency")
@AllArgsConstructor
@Getter
@Tag(name = "能耗有效性1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "energy-efficiency", name = "能耗有效性")
@Slf4j
public class EnergyEfficiencyController {

    private final ElectricityConsumeService electricityConsumeService;

    private final QueryHelper queryHelper;

    private final TestRecordService testRecordService;

    private final TestEnergyDetailService testEnergyDetailService;

    private final TestConfigDeviceService testConfigDeviceService;

    @PostMapping("/char")
    @Operation(summary = "获取有效试验和无效试验能耗图表")
    @QueryAction
    public Mono<HashMap<String,Object>> energyEfficiencyAnalysis(@RequestBody EnergyEfficiencyReq energyEfficiencyReq){
        //获取该段时间内的总能耗
        HashMap<String, Object> resultMap = new HashMap<>();
        Long[] longs={energyEfficiencyReq.getStartDate(),energyEfficiencyReq.getEndDate()};
        return  queryHelper.select("select sum(difference) as totalEnergy,sum(unit_price*difference) as totalCost from sems_electricity_consume where device_id='0'", EfficiencyRes::new)
            .where(dsl->dsl.and("gather_time","btw",longs).noPaging())
            .fetch()
            .collectList()
            .flatMap(data->{
                if(data.isEmpty()){
                    return Mono.empty();
                }
                EfficiencyRes efficiencyRes = data.get(0);
                BigDecimal totalEnergy = efficiencyRes.getTotalEnergy()==null?BigDecimal.ZERO:efficiencyRes.getTotalEnergy();
                BigDecimal cost = efficiencyRes.getTotalCost()==null?BigDecimal.ZERO:efficiencyRes.getTotalCost();
                return this.getEnergyAndCost(energyEfficiencyReq.getStartDate(), energyEfficiencyReq.getEndDate())
                    .flatMap(value->{
                        resultMap.put("efficiency",value);

                        BigDecimal effectiveEnergy = value.getEnergy()==null?BigDecimal.ZERO:value.getEnergy().setScale(2,RoundingMode.HALF_UP);
                        BigDecimal effectiveCost = value.getCost()==null?BigDecimal.ZERO:value.getCost().setScale(2,RoundingMode.HALF_UP);

                        BigDecimal invalidEnergy = totalEnergy.subtract(effectiveEnergy).setScale(2, RoundingMode.HALF_UP);
                        BigDecimal invalidCost = cost.subtract(effectiveCost).setScale(2,RoundingMode.HALF_UP);
                        EnergyEfficiencyRes invalid = new EnergyEfficiencyRes();

                        invalid.setEnergy(invalidEnergy);
                        invalid.setCost(invalidCost);
                        resultMap.put("invalid",invalid);
                        EnergyEffectiveReportRes energyEffectiveReportRes = new EnergyEffectiveReportRes();
                        energyEffectiveReportRes.setTime(DateUtil.dateToString(new Date(energyEfficiencyReq.getStartDate()),DateUtil.DATE_SHORT_FORMAT)+" ~ "+DateUtil.dateToString(new Date(energyEfficiencyReq.getEndDate()),DateUtil.DATE_SHORT_FORMAT));
                        energyEffectiveReportRes.setEffectiveEnergy(value.getEnergy().setScale(2,RoundingMode.HALF_UP));
                        energyEffectiveReportRes.setEffectiveCost(value.getCost().setScale(2,RoundingMode.HALF_UP));
                        energyEffectiveReportRes.setInvalidEnergy(invalidEnergy);
                        energyEffectiveReportRes.setInvalidCost(invalidCost);
                        resultMap.put("report",energyEffectiveReportRes);
                        return Mono.just(resultMap);
                    });

            });
    }

    /**
     * 试验能耗计算
     * @param startDate
     * @param endDate
     * @return
     */
    public Mono<EnergyEfficiencyRes> getEnergyAndCost(Long startDate, Long endDate){
        EnergyEfficiencyRes energyEfficiencyRes = new EnergyEfficiencyRes();
        energyEfficiencyRes.setEnergy(BigDecimal.ZERO);
        energyEfficiencyRes.setCost(BigDecimal.ZERO);
        //1.获取该段时间内的试验
        return queryHelper.select("SELECT * FROM sems_test_record WHERE not (test_start_time > " +endDate+ " or test_end_time < " + startDate+ ")", TestRecordEntity::new)

            .where(new QueryParamEntity().noPaging())
            .fetch()
            .flatMap(record->{
                //根据试验获取该试验对应条目的设备
                return testEnergyDetailService
                    .createQuery()
                    .where(TestEnergyDetailEntity::getTestRecordId,record.getId())
                    .fetch()
                    .map(TestEnergyDetailEntity::getDeviceId)
                    .collectList()
                    .flatMap(deviceIds->{
                        //计算交叉时间
                        HashMap<String, DateTime> map = testConfigDeviceService.setOverlap(new DateTime(startDate), new DateTime(endDate), new DateTime(record.getTestStartTime()), new DateTime(record.getTestEndTime()));

                        //用量
                        Long[] longs={map.get("startDate").toDate().getTime(),map.get("endDate").toDate().getTime()};
                        return  queryHelper.select("select sum(difference) as totalEnergy,sum(unit_price*difference) as totalCost from sems_electricity_consume", EfficiencyRes::new)
                            .where(dsl->dsl.and("gather_time","btw",longs).and("device_id","in",deviceIds).noPaging())
                            .fetch()
                            .collectList()
                            .flatMap(v -> {
                                EfficiencyRes efficiencyRes = v.get(0);
                                BigDecimal total = efficiencyRes.getTotalEnergy()==null?BigDecimal.ZERO:efficiencyRes.getTotalEnergy();
                                BigDecimal cost = efficiencyRes.getTotalCost()==null?BigDecimal.ZERO:efficiencyRes.getTotalCost();
                                energyEfficiencyRes.setEnergy(energyEfficiencyRes.getEnergy().add(total));
                                energyEfficiencyRes.setCost(energyEfficiencyRes.getCost().add(cost));
                                return Mono.just(energyEfficiencyRes);
                            });
                    });

            }).then(Mono.just(energyEfficiencyRes));
    }
}
