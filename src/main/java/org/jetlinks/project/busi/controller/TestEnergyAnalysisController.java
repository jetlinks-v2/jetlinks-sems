package org.jetlinks.project.busi.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.*;
import org.jetlinks.project.busi.entity.req.AnalysitItemReq;
import org.jetlinks.project.busi.entity.req.TestAnalysisReq;
import org.jetlinks.project.busi.entity.res.*;
import org.jetlinks.project.busi.service.*;
import org.jetlinks.project.busi.utils.DateUtil;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/test/analysis")
@AllArgsConstructor
@Getter
@Tag(name = "试验能耗分析 1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "test_analysis", name = "试验能耗分析")
public class TestEnergyAnalysisController {

    private final TestRecordService testRecordService;

    private final TestEnergyDetailService testEnergyDetailService;

    private final TestConfigDeviceService testConfigDeviceService;

    private final ElectricityConsumeService electricityConsumeService;

    private final QueryHelper queryHelper;

    private final DeviceService deviceService;

    private final  TestAreaService testAreaService;

    private final EnergyRatioService energyRatioService;

    @Operation(summary = "试验场所分析")
    @PostMapping("/testAreaAnalysis")
    @QueryAction
    public Mono<HashMap<String,Object>> testAnalysis(@RequestBody TestAnalysisReq testAnalysisReq){
        DecimalFormat format = new DecimalFormat("##%");
        HashMap<String, Object> resultMap = new HashMap<>();
        Long startDate = testAnalysisReq.getStartDate();
        Long endDate = testAnalysisReq.getEndDate();

        TestAnalysisReportRes testAnalysisReportRes = new TestAnalysisReportRes();
        testAnalysisReportRes.setTime(DateUtil.dateToString(new Date(testAnalysisReq.getStartDate()),DateUtil.DATE_SHORT_FORMAT)+"-"+DateUtil.dateToString(new Date(testAnalysisReq.getEndDate()),DateUtil.DATE_SHORT_FORMAT));

        return this.getEnergyExtend(startDate,endDate)
            .flatMap(va->{
                //计算总量及比例
                BigDecimal total = va.stream().map(EnergyChartRes::getNum).reduce(BigDecimal.ZERO, BigDecimal::add);
                if(total.compareTo(BigDecimal.ZERO)==0){
                    resultMap.put("total",BigDecimal.ZERO);
                    //展示所有场所
                    return testAreaService
                        .query(new QueryParamEntity())
                        .map(TestAreaEntity::getAreaName)
                        .flatMap(li->{
                            EnergyChartRes energyChartRes = new EnergyChartRes();
                            energyChartRes.setName(li);
                            energyChartRes.setNum(BigDecimal.ZERO);
                            energyChartRes.setRate("0");
                            return Mono.just(energyChartRes);
                        }).collectList()
                        .doOnNext(v-> {
                            EnergyChartRes energyChartRes = v.get(0);
                            testAnalysisReportRes.setAreaName(energyChartRes.getName());
                            testAnalysisReportRes.setRate(energyChartRes.getRate());
                            testAnalysisReportRes.setTotal(energyChartRes.getNum()==null?BigDecimal.ZERO:energyChartRes.getNum());
                            testAnalysisReportRes.setCost(energyChartRes.getCost()==null?BigDecimal.ZERO:energyChartRes.getCost());
                            resultMap.put("list",v);
                        })
                        .thenReturn(va);
                }else {
                    resultMap.put("total",total);
                    return Flux.fromIterable(va)
                        .flatMap(energyChartRes->{
                            BigDecimal divide = energyChartRes.getNum().divide(total, 2, BigDecimal.ROUND_HALF_UP);
                            energyChartRes.setRate(format.format(divide));
                            return testAreaService
                                .findById(energyChartRes.getName())
                                .doOnNext(val->energyChartRes.setName(val.getAreaName()))
                                .thenReturn(energyChartRes);
                        }).collectList()
                        .doOnNext(v-> {
                            EnergyChartRes energyChartRes = v.stream().max(Comparator.comparing(EnergyChartRes::getNum)).get();
                            testAnalysisReportRes.setAreaName(energyChartRes.getName());
                            testAnalysisReportRes.setRate(energyChartRes.getRate());
                            testAnalysisReportRes.setTotal(energyChartRes.getNum()==null?BigDecimal.ZERO:energyChartRes.getNum().setScale(2,RoundingMode.HALF_UP));
                            testAnalysisReportRes.setCost(energyChartRes.getCost()==null?BigDecimal.ZERO:energyChartRes.getCost().setScale(2,RoundingMode.HALF_UP));
                            resultMap.put("list",v);
                        })
                        .thenReturn(va);
                }
            })
            .flatMap(v1->
                this.yoyAndQoqAreaAnalysis(testAnalysisReq,v1)
                    .sort(Comparator.comparing(TestAreaAnalysisRes::getEnergy))
                    .collectList()
                    .flatMap(value->{
                        if(value.isEmpty()){
                            //展示所有场所
                            return testAreaService
                                .query(new QueryParamEntity())
                                .map(TestAreaEntity::getAreaName)
                                .flatMap(li->{
                                    TestAreaAnalysisRes testAreaAnalysisRes = new TestAreaAnalysisRes();
                                    testAreaAnalysisRes.setAreaName(li);
                                    testAreaAnalysisRes.setYoy("0");
                                    testAreaAnalysisRes.setEnergy(BigDecimal.ZERO);
                                    testAreaAnalysisRes.setQoq("0");
                                    return Mono.just(testAreaAnalysisRes);
                                }).collectList()
                                .doOnNext(v-> {
                                    resultMap.put("report",testAnalysisReportRes);
                                    resultMap.put("analysis",v);
                                })
                                .thenReturn(resultMap);
                        }else {
                            TestAreaAnalysisRes testAreaAnalysisRes = value.get(0);
                            String qoq = testAreaAnalysisRes.getQoq();
                            String yoy = testAreaAnalysisRes.getYoy();
                            Number qoqs =0;
                            Number yoys =0;
                            try {
                                qoqs =format.parse(qoq);
                                yoys = format.parse(yoy);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            testAnalysisReportRes.setRemarkArea(testAreaAnalysisRes .getAreaName());
                            if(qoqs.doubleValue()>yoys.doubleValue()){

                                testAnalysisReportRes.setRemark(qoqs);
                            }else {
                                testAnalysisReportRes.setRemark(yoys);
                            }
                            resultMap.put("analysis",value);
                            resultMap.put("report",testAnalysisReportRes);
                            return Mono.just(resultMap);
                        }
                    })
            );
    }

    //不考虑公共设备的能耗的方法，后面公共设备的概念都好了之后用getEnergyExtend()
    public Mono<List<EnergyChartRes>> getEnergy(Long startDate,Long endDate){
        //1.获取该段时间内的试验
        return queryHelper.select("SELECT * FROM sems_test_record WHERE not (test_start_time > " +endDate+ " or test_end_time < " + startDate + ")", TestRecordEntity::new)

            .where(new QueryParamEntity())
            .fetch()
            .flatMap(record->{
                //根据试验获取该试验对应条目的设备
                return testEnergyDetailService
                    .createQuery()
                    .where(TestEnergyDetailEntity::getTestRecordId,record.getId())
                    .fetch()
                    .map(TestEnergyDetailEntity::getDeviceId)
                    .flatMap(deviceIds->{
                        //根据设备统计场所能耗
                        HashMap<String, DateTime> map = testConfigDeviceService.setOverlap(new DateTime(startDate), new DateTime(endDate), new DateTime(record.getTestStartTime()), new DateTime(record.getTestEndTime()));

                        return deviceService.createQuery()
                            .in(DeviceInfoEntity::getDeviceId,deviceIds)
                            .fetch()
                            .filter(i->i.getPlaceId() != null)
                            .collect(Collectors.groupingBy(DeviceInfoEntity::getPlaceId))
                            .flatMapMany(deviceMap-> {
                                return Flux.fromIterable(deviceMap.entrySet())
                                    .flatMap(ma -> {
                                        EnergyChartRes energyChartRes = new EnergyChartRes();
                                        //用量
                                        List<String> areaDeviceIds = ma.getValue().stream().map(DeviceInfoEntity::getDeviceId).collect(Collectors.toList());
                                        return testAreaService.findById(ma.getKey())
                                            .flatMap(valie->{
                                                energyChartRes.setName(valie.getAreaName());
                                                return electricityConsumeService
                                                    .createQuery()
                                                    .in(ElectricityConsumeEntity::getDeviceId, areaDeviceIds)
                                                    .lte(ElectricityConsumeEntity::getGatherTime, map.get("endDate").toDate().getTime())
                                                    .gte(ElectricityConsumeEntity::getGatherTime, map.get("startDate").toDate().getTime())
                                                    .fetch()
                                                    .doOnNext(val->
                                                        val.setUnitPrice(val.getUnitPrice().multiply(val.getDifference())))
                                                    .collectList()
                                                    .flatMap(list->{
                                                        BigDecimal cost = list.stream().map(ElectricityConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                                                        BigDecimal total = list.stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add);
                                                        energyChartRes.setNum(total);
                                                        energyChartRes.setCost(cost);
                                                        return Mono.just(energyChartRes);
                                                    });
                                            });

                                    });
                            });

                    });

            }).collectList()
            .flatMap(li->{
                List<EnergyChartRes> merge = merge(li);
                return Mono.just(merge);
            });

    }


    //包含公用设备和普通设备的能耗计算，等后面客户体验系统的数据进来后，能耗占比这些都配置好之后，把场所分析里面的getExergy替换为虾米这个。
    public Mono<List<EnergyChartRes>> getEnergyExtend(Long startDate,Long endDate){
        //1.获取该段时间内的试验
        return queryHelper.select("SELECT * FROM sems_test_record WHERE not (test_start_time > " +endDate+ " or test_end_time < " + startDate + ")", TestRecordEntity::new)

            .where(new QueryParamEntity())
            .fetch()
            .flatMap(record->{
                //根据试验获取该试验对应条目的设备
                return testEnergyDetailService
                    .createQuery()
                    .where(TestEnergyDetailEntity::getTestRecordId,record.getId())
                    .fetch()
                    .collect(Collectors.groupingBy(TestEnergyDetailEntity::getShareDevice))
                    .flatMapMany(shareMap->{
                        //根据设备统计场所能耗
                        HashMap<String, DateTime> map = testConfigDeviceService.setOverlap(new DateTime(startDate), new DateTime(endDate), new DateTime(record.getTestStartTime()), new DateTime(record.getTestEndTime()));
                        return Flux.fromIterable(shareMap.entrySet())
                            .flatMap(value->{
                                if("0".equals(value.getKey())){
                                    //非共用设备
                                    List<String> deviceIds = value.getValue().stream().map(TestEnergyDetailEntity::getDeviceId).collect(Collectors.toList());
                                    return deviceService.createQuery()
                                        .in(DeviceInfoEntity::getDeviceId,deviceIds)
                                        .fetch()
                                        .filter(i->i.getPlaceId() != null)
                                        .collect(Collectors.groupingBy(DeviceInfoEntity::getPlaceId))
                                        .flatMapMany(deviceMap->{
                                                return Flux.fromIterable(deviceMap.entrySet())
                                                    .flatMap(ma->{
                                                        return testAreaService.findById(ma.getKey())
                                                            .flatMap(valie->{
                                                                EnergyChartRes energyChartRes = new EnergyChartRes();
                                                                energyChartRes.setName(valie.getAreaName());
                                                                //用量
                                                                List<String> areaDeviceIds = ma.getValue().stream().map(DeviceInfoEntity::getDeviceId).collect(Collectors.toList());
                                                                return electricityConsumeService
                                                                    .createQuery()
                                                                    .in(ElectricityConsumeEntity::getDeviceId,areaDeviceIds)
                                                                    .lte(ElectricityConsumeEntity::getGatherTime,map.get("endDate").toDate().getTime())
                                                                    .gte(ElectricityConsumeEntity::getGatherTime,map.get("startDate").toDate().getTime())
                                                                    .fetch()
                                                                    .doOnNext(val->
                                                                        val.setUnitPrice(val.getUnitPrice().multiply(val.getDifference())))
                                                                    .collectList()
                                                                    .flatMap(v->{
                                                                        //总能耗
                                                                        BigDecimal total = v.stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                                                        //总费用
                                                                        BigDecimal cost = v.stream().map(ElectricityConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                                                        energyChartRes.setNum(total);
                                                                        energyChartRes.setCost(cost);
                                                                        return Mono.just(energyChartRes);
                                                                    });
                                                            });
                                                    });


                                            });
                                }else {
                                    //共用设备
                                    List<TestEnergyDetailEntity> unitDevice = value.getValue();
                                    return energyRatioService
                                        .createQuery()
                                        .where(EnergyRatioEntity::getConfigId,record.getConfigId())
                                        .fetchOne()
                                        .flatMapMany(ration->{
                                            return Flux.fromIterable(unitDevice)
                                                .flatMap(li->{
                                                    return deviceService.createQuery()
                                                        .where(DeviceInfoEntity::getDeviceId,li.getDeviceId())
                                                        .fetch()
                                                        .filter(i->i.getPlaceId() != null)
                                                        .flatMap(deviceInfo->{
                                                            if(li.getDeviceId().equals("C0505217") || li.getDeviceId().equals("C0505218")){
                                                                //普冷
                                                                List<EnergyRatioRes> ordinaryFreezing = ration.getOrdinaryFreezing();
                                                                EnergyRatioRes energyRatioRes = ordinaryFreezing.stream().filter(i -> i.getPlaceId().equals(deviceInfo.getDeviceId())).findFirst().orElseGet(EnergyRatioRes::new);
                                                                //计算能耗
                                                                return testAreaService.findById(deviceInfo.getPlaceId())
                                                                    .flatMap(valie-> {
                                                                        EnergyChartRes energyChartRes = new EnergyChartRes();
                                                                        energyChartRes.setName(valie.getAreaName());
                                                                        String reportDevice = null;
                                                                        if (li.getDeviceId().equals("C0505217")) {
                                                                            reportDevice = "CWT1-13";
                                                                        } else {
                                                                            reportDevice = "CWT1-12";
                                                                        }
                                                                        return electricityConsumeService
                                                                            .createQuery()
                                                                            .where(ElectricityConsumeEntity::getDeviceId, li.getDeviceId())
                                                                            .where(ElectricityConsumeEntity::getReportDeviceId, reportDevice)
                                                                            .lte(ElectricityConsumeEntity::getGatherTime, map.get("endDate").toDate().getTime())
                                                                            .gte(ElectricityConsumeEntity::getGatherTime, map.get("startDate").toDate().getTime())
                                                                            .fetch()
                                                                            .doOnNext(val->
                                                                                val.setUnitPrice(val.getUnitPrice().multiply(val.getDifference())))
                                                                            .collectList()
                                                                            .flatMap(v->{
                                                                                //总能耗
                                                                                BigDecimal total = v.stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                                                                //总费用
                                                                                BigDecimal cost = v.stream().map(ElectricityConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                                                                energyChartRes.setNum(total.multiply(energyRatioRes.getEnergyRadio()==null?BigDecimal.ONE:energyRatioRes.getEnergyRadio()));
                                                                                energyChartRes.setCost(cost.multiply(energyRatioRes.getEnergyRadio()==null?BigDecimal.ONE:energyRatioRes.getEnergyRadio()));
                                                                                return Mono.just(energyChartRes);
                                                                            });
                                                                        });
                                                                    }else if(li.getDeviceId().equals("C0801008") ){
                                                                    //组合开始冷却塔
                                                                List<EnergyRatioRes> ordinaryFreezing = ration.getCombinedCoolingTowerAuto();
                                                                    EnergyRatioRes energyRatioRes = ordinaryFreezing.stream().filter(i -> i.getPlaceId().equals(deviceInfo.getDeviceId())).findFirst().orElseGet(EnergyRatioRes::new);
                                                                    //计算能耗
                                                                    return testAreaService.findById(deviceInfo.getPlaceId())
                                                                        .flatMap(valie-> {
                                                                            EnergyChartRes energyChartRes = new EnergyChartRes();
                                                                            energyChartRes.setName(valie.getAreaName());

                                                                            return electricityConsumeService
                                                                                .createQuery()
                                                                                .where(ElectricityConsumeEntity::getDeviceId, li.getDeviceId())
                                                                                .where(ElectricityConsumeEntity::getReportDeviceId, "CWT2-14")
                                                                                .lte(ElectricityConsumeEntity::getGatherTime, map.get("endDate").toDate().getTime())
                                                                                .gte(ElectricityConsumeEntity::getGatherTime, map.get("startDate").toDate().getTime())
                                                                                .fetch()
                                                                                .doOnNext(val->
                                                                                    val.setUnitPrice(val.getUnitPrice().multiply(val.getDifference())))
                                                                                .collectList()
                                                                                .flatMap(v-> {
                                                                                    //总能耗
                                                                                    BigDecimal total = v.stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                                                                                    //总费用
                                                                                    BigDecimal cost = v.stream().map(ElectricityConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                                                                                    energyChartRes.setNum(total.multiply(energyRatioRes.getEnergyRadio() == null ? BigDecimal.ONE : energyRatioRes.getEnergyRadio()).setScale(2,RoundingMode.HALF_UP));
                                                                                    energyChartRes.setCost(cost.multiply(energyRatioRes.getEnergyRadio() == null ? BigDecimal.ONE : energyRatioRes.getEnergyRadio()).setScale(2,RoundingMode.HALF_UP));
                                                                                    return Mono.just(energyChartRes);
                                                                                });

                                                                        });
                                                                }else if(li.getDeviceId().equals("C0507049")) {
                                                                //锅炉
                                                                List<EnergyRatioRes> ordinaryFreezing = ration.getBoiler();
                                                                EnergyRatioRes energyRatioRes = ordinaryFreezing.stream().filter(i -> i.getPlaceId().equals(deviceInfo.getDeviceId())).findFirst().orElseGet(EnergyRatioRes::new);
                                                                //计算能耗
                                                                return testAreaService.findById(deviceInfo.getPlaceId())
                                                                    .flatMap(valie-> {
                                                                        EnergyChartRes energyChartRes = new EnergyChartRes();
                                                                        energyChartRes.setName(valie.getAreaName());

                                                                        return electricityConsumeService
                                                                            .createQuery()
                                                                            .where(ElectricityConsumeEntity::getDeviceId, li.getDeviceId())
                                                                            .where(ElectricityConsumeEntity::getReportDeviceId, "CWT2-5")
                                                                            .lte(ElectricityConsumeEntity::getGatherTime, map.get("endDate").toDate().getTime())
                                                                            .gte(ElectricityConsumeEntity::getGatherTime, map.get("startDate").toDate().getTime())
                                                                            .fetch()
                                                                            .doOnNext(val->
                                                                                val.setUnitPrice(val.getUnitPrice().multiply(val.getDifference())))
                                                                            .collectList()
                                                                            .flatMap(v-> {
                                                                                //总能耗
                                                                                BigDecimal total = v.stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                                                                                //总费用
                                                                                BigDecimal cost = v.stream().map(ElectricityConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                                                                                energyChartRes.setNum(total.multiply(energyRatioRes.getEnergyRadio() == null ? BigDecimal.ONE : energyRatioRes.getEnergyRadio()).setScale(2,RoundingMode.HALF_UP));
                                                                                energyChartRes.setCost(cost.multiply(energyRatioRes.getEnergyRadio() == null ? BigDecimal.ONE : energyRatioRes.getEnergyRadio()).setScale(2,RoundingMode.HALF_UP));
                                                                                return Mono.just(energyChartRes);
                                                                            });
                                                                    });
                                                            }else if(li.getDeviceId().equals("A0401056")) {
                                                                //空压冷却塔（现在还没有气动和声学）
                                                                List<EnergyRatioRes> ordinaryFreezing = ration.getAirCompressor();
                                                                EnergyRatioRes energyRatioRes = ordinaryFreezing.stream().filter(i -> i.getPlaceId().equals(deviceInfo.getDeviceId())).findFirst().orElseGet(EnergyRatioRes::new);
                                                                //计算能耗
                                                                return testAreaService.findById(deviceInfo.getPlaceId())
                                                                    .flatMap(valie-> {
                                                                        EnergyChartRes energyChartRes = new EnergyChartRes();
                                                                        energyChartRes.setName(valie.getAreaName());

                                                                        return electricityConsumeService
                                                                            .createQuery()
                                                                            .where(ElectricityConsumeEntity::getDeviceId, li.getDeviceId())
                                                                            .where(ElectricityConsumeEntity::getReportDeviceId, "AAWT2-9")
                                                                            .lte(ElectricityConsumeEntity::getGatherTime, map.get("endDate").toDate().getTime())
                                                                            .gte(ElectricityConsumeEntity::getGatherTime, map.get("startDate").toDate().getTime())
                                                                            .fetch()
                                                                            .doOnNext(val->
                                                                                val.setUnitPrice(val.getUnitPrice().multiply(val.getDifference())))
                                                                            .collectList()
                                                                            .flatMap(v-> {
                                                                                //总能耗
                                                                                BigDecimal total = v.stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                                                                                //总费用
                                                                                BigDecimal cost = v.stream().map(ElectricityConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                                                                                energyChartRes.setNum(total.multiply(energyRatioRes.getEnergyRadio() == null ? BigDecimal.ONE : energyRatioRes.getEnergyRadio()).setScale(2,RoundingMode.HALF_UP));
                                                                                energyChartRes.setCost(cost.multiply(energyRatioRes.getEnergyRadio() == null ? BigDecimal.ONE : energyRatioRes.getEnergyRadio()).setScale(2,RoundingMode.HALF_UP));
                                                                                return Mono.just(energyChartRes);
                                                                            });
                                                                    });

                                                            }else {
                                                                //计算能耗
                                                                return testAreaService.findById(deviceInfo.getPlaceId())
                                                                    .flatMap(valie-> {
                                                                        EnergyChartRes energyChartRes = new EnergyChartRes();
                                                                        energyChartRes.setName(valie.getAreaName());

                                                                        return electricityConsumeService
                                                                            .createQuery()
                                                                            .where(ElectricityConsumeEntity::getDeviceId, li.getDeviceId())
                                                                            .lte(ElectricityConsumeEntity::getGatherTime, map.get("endDate").toDate().getTime())
                                                                            .gte(ElectricityConsumeEntity::getGatherTime, map.get("startDate").toDate().getTime())
                                                                            .fetch()
                                                                            .doOnNext(val->
                                                                                val.setUnitPrice(val.getUnitPrice().multiply(val.getDifference())))
                                                                            .collectList()
                                                                            .flatMap(v-> {
                                                                                //总能耗
                                                                                BigDecimal total = v.stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                                                                                //总费用
                                                                                BigDecimal cost = v.stream().map(ElectricityConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                                                                                energyChartRes.setNum(total);
                                                                                energyChartRes.setCost(cost);
                                                                                return Mono.just(energyChartRes);
                                                                            });
                                                                    });
                                                            }
                                                            });
                                                        });
                                                });
                                }
                            });
                    });

            }).collectList()
            .flatMap(li->{
                List<EnergyChartRes> merge = merge(li);
                return Mono.just(merge);
            });
    }

    //组合数据
    public static List<EnergyChartRes> merge(List<EnergyChartRes> list) {
        List<EnergyChartRes> result = list.stream()
            // 表示name为key， 接着如果有重复的，那么从Singer对象o1与o2中筛选出一个，这里选择o1，
            // 并把name重复，需要将ages与o1进行合并的o2, 赋值给o1，最后返回o1
            .collect(Collectors.toMap(EnergyChartRes::getName, a -> a, (o1, o2) -> {
                o1.setNum(o1.getNum().add(o2.getNum()));
                o1.setCost(o1.getCost().add(o2.getCost()));
                return o1;
            })).values().stream().collect(Collectors.toList());
        return result;

    }




    //同比环比计算
    public Flux<TestAreaAnalysisRes> yoyAndQoqAreaAnalysis( TestAnalysisReq testAnalysisReq,List<EnergyChartRes> listEnergy){
        DecimalFormat format = new DecimalFormat("##%");
        //时间处理
        //搜索开始时间
        Long startDate = testAnalysisReq.getStartDate();
        //搜索结束时间
        Long endDate = testAnalysisReq.getEndDate();
        //搜索开始时间对应去年开始时间
        Date lastYearStart = DateUtil.stringToDate(DateUtil.addYears(new Date(startDate), -1),DateUtil.DATE_SHORT_FORMAT);
        //搜索结束时间对应的去年结束时间
        Date lastYearEnd = DateUtil.stringToDate(DateUtil.addYears(new Date(endDate), -1),DateUtil.DATE_SHORT_FORMAT);
        //计算同期开始时间与结束时间
        int i = DateUtil.daysOfTwo(new Date(startDate), new Date(endDate));
        Date lastStart = DateUtil.stringToDate(DateUtil.addDay(new Date(startDate), -i),DateUtil.DATE_SHORT_FORMAT);


        ArrayList<TestAreaAnalysisRes> resultList = new ArrayList<>();
        return Mono.just(listEnergy)
            .flatMapMany(list->{
                //同比
                assert lastYearStart != null;
                assert lastYearEnd != null;
                return this.getEnergy(lastYearStart.getTime(),lastYearEnd.getTime())
                    .flatMapMany(lastYearValueList->{
                        return this.getEnergy(lastStart.getTime(),startDate-1)
                            .flatMapMany(lastValueList-> {
                                for (EnergyChartRes energyChartRes : list) {
                                    TestAreaAnalysisRes testAreaAnalysisRes = new TestAreaAnalysisRes();
                                    testAreaAnalysisRes.setAreaName(energyChartRes.getName());
                                    testAreaAnalysisRes.setEnergy(energyChartRes.getNum());
                                    testAreaAnalysisRes.setQoq("0%");
                                    testAreaAnalysisRes.setYoy("0%");
                                    //计算同比
                                    for (EnergyChartRes chartRes : lastYearValueList) {
                                        if(chartRes.getName().equals(energyChartRes.getName())){
                                            if(chartRes.getNum().compareTo(BigDecimal.ZERO)==0){

                                                testAreaAnalysisRes.setYoy(format.format(BigDecimal.valueOf(1)));
                                            }else {
                                                BigDecimal divide = energyChartRes.getNum().subtract(chartRes.getNum()).divide(chartRes.getNum(), 2, BigDecimal.ROUND_HALF_UP);
                                                testAreaAnalysisRes.setYoy(format.format(divide));
                                            }
                                        }
                                    }
                                    //计算环比
                                    for (EnergyChartRes chartRes : lastValueList) {
                                        if(chartRes.getName().equals(energyChartRes.getName())){
                                            if(chartRes.getNum().compareTo(BigDecimal.ZERO)==0){

                                                testAreaAnalysisRes.setQoq(format.format(BigDecimal.valueOf(0)));
                                            }else {
                                                BigDecimal divide = energyChartRes.getNum().subtract(chartRes.getNum()).divide(chartRes.getNum(), 2, BigDecimal.ROUND_HALF_UP);
                                                testAreaAnalysisRes.setQoq(format.format(divide));
                                            }
                                        }
                                    }
                                    resultList.add(testAreaAnalysisRes);
                                }
                                return Flux.fromIterable(resultList);
                            });
                    });
            });

    }

    @Operation(summary = "试验对比分析")
    @PostMapping("/test/analysis")
    @QueryAction
    public Mono<HashMap<String,Object>> testCompare(@RequestBody AnalysitItemReq analysitItemReq) {
        List<TestConfigEntity> itemList = analysitItemReq.getItemList();
        List<String> stringStream = itemList.stream().map(TestConfigEntity::getTestName).collect(Collectors.toList());


        //判断时间范围
        String startTime = analysitItemReq.getStartTime()+" 00:00:00";
        analysitItemReq.setStartTime(startTime);
        String endTime = analysitItemReq.getEndTime()+" 23:59:59";
        analysitItemReq.setEndTime(endTime);
        int days = DateUtil.daysOfTwo(DateUtil.stringToDate(startTime, DateUtil.DATE_WITHSECOND_FORMAT), DateUtil.stringToDate(endTime, DateUtil.DATE_WITHSECOND_FORMAT));
        if (days <= 1) {
            return this.dayTestCompare(analysitItemReq);
        }else if(days<=31){
            List<TestEnergyAndCostRes> timeByDate = this.getTimeByDate(DateUtil.stringToDate(analysitItemReq.getStartTime()), DateUtil.stringToDate(analysitItemReq.getEndTime()), days);
            HashMap<String, List<TestEnergyAndCostRes>> stringTestEnergyAndCostResHashMap = new HashMap<>();
            return Flux.fromIterable(timeByDate)
                .flatMap(data->{
                    return this.getEnergyAndCostExtend(data.getTimeStart(),data.getTimeEnd(),data)
                        .filter(i->stringStream.contains(i.getItemName()))
                        .collectList()
                        .doOnNext(value->stringTestEnergyAndCostResHashMap.put(data.getTime(),value))
                        .thenReturn(stringTestEnergyAndCostResHashMap)
                        .then();
                }).collectList()
                .then(this.monthTestCompareExtend(stringTestEnergyAndCostResHashMap,analysitItemReq));
        }else {
            List<TestEnergyAndCostRes> timeByDate = this.getTimeByDate(DateUtil.stringToDate(analysitItemReq.getStartTime()), DateUtil.stringToDate(analysitItemReq.getEndTime()), days);
            HashMap<String, List<TestEnergyAndCostRes>> stringTestEnergyAndCostResHashMap = new HashMap<>();
            return Flux.fromIterable(timeByDate)
                .flatMap(data->{
                    return this.monthGetEnergyAndCost(data.getTimeStart(),data.getTimeEnd(),data)
                        .filter(i->stringStream.contains(i.getItemName()))
                        .collectList()
                        .doOnNext(value->stringTestEnergyAndCostResHashMap.put(data.getTime(),value))
                        .thenReturn(stringTestEnergyAndCostResHashMap)
                        .then();
                }).collectList()
                .then(this.monthTestCompareExtend(stringTestEnergyAndCostResHashMap,analysitItemReq));
        }
    }

    public Mono<HashMap<String,Object>> dayTestCompare(AnalysitItemReq analysitItemReq){
        List<TestConfigEntity> itemList = analysitItemReq.getItemList();
        List<String> stringStream = itemList.stream().map(TestConfigEntity::getTestName).collect(Collectors.toList());

        HashMap<String, Object> map = new HashMap<>();
        TestCompareReportRes testCompareReportRes = new TestCompareReportRes();
        return Flux.fromIterable(itemList)
            .flatMap(va->{
                return this.getEnergyAndCost(DateUtil.stringToDate(analysitItemReq.getStartTime(),DateUtil.DATE_WITHSECOND_FORMAT ).getTime(),DateUtil.stringToDate(analysitItemReq.getEndTime(),DateUtil.DATE_WITHSECOND_FORMAT ).getTime())
                    .filter(vao->stringStream.contains(vao.getItemName()));
            }).collectList()
            .flatMap(list->{
                List<TestEnergyAndCostRes> testEnergyAndCostRes1 = this.mergeTest(list);
                Map<String, TestEnergyAndCostRes> collect = testEnergyAndCostRes1.stream().collect(Collectors.toMap(TestEnergyAndCostRes::getItemName, i->i));
                ArrayList<TestEnergyAndCostRes> testEnergyAndCostResE = new ArrayList<>();
                for (TestConfigEntity testConfigEntity : itemList) {
                    if(collect.get(testConfigEntity.getTestName())== null){
                        TestEnergyAndCostRes testEnergyAndCostRes = new TestEnergyAndCostRes();
                        testEnergyAndCostRes.setItemName(testConfigEntity.getTestName());
                        testEnergyAndCostRes.setCost(BigDecimal.ZERO);
                        testEnergyAndCostRes.setNumber(BigDecimal.ZERO);
                        testEnergyAndCostResE.add(testEnergyAndCostRes);
                    }else {
                        testEnergyAndCostResE.add(collect.get(testConfigEntity.getTestName()));
                    }
                }
                map.put("list",testEnergyAndCostResE);
                if(testEnergyAndCostResE.isEmpty()){
                    return Mono.just(map);
                }
                ArrayList<TestAnalysisReportRes> reportList = new ArrayList<>();
                TestEnergyAndCostRes max=null;
                for (TestEnergyAndCostRes testEnergyAndCostRes : testEnergyAndCostRes1) {
                    if(max==null){
                        max=testEnergyAndCostRes;
                    }else {
                        if(testEnergyAndCostRes.getNumber().compareTo(max.getNumber())>0){
                            max=testEnergyAndCostRes;
                        }
                    }
                    TestAnalysisReportRes testAnalysisReportRes = new TestAnalysisReportRes();
                    testAnalysisReportRes.setTime(analysitItemReq.getStartTime()+"~"+analysitItemReq.getEndTime());
                    testAnalysisReportRes.setAreaName(testEnergyAndCostRes.getItemName());
                    testAnalysisReportRes.setTotal(testEnergyAndCostRes.getNumber().setScale(2,RoundingMode.HALF_UP));
                    testAnalysisReportRes.setCost(testEnergyAndCostRes.getCost().setScale(2,RoundingMode.HALF_UP));
                    reportList.add(testAnalysisReportRes);
                }
                testCompareReportRes.setList(reportList);
                if(max!=null){
                    testCompareReportRes.setAreaName(max.getItemName());
                    ArrayList<String> strings = new ArrayList<>();
                    for (TestEnergyAndCostRes testEnergyAndCostRes : testEnergyAndCostRes1) {
                        BigDecimal number = max.getNumber();
                        if(!testEnergyAndCostRes.equals(max)){
                            BigDecimal divide =BigDecimal.ZERO;
                            if(testEnergyAndCostRes.getNumber().compareTo(BigDecimal.ZERO)==0){
                                divide=BigDecimal.ZERO;
                            }else {
                                divide = number.subtract(testEnergyAndCostRes.getNumber()).divide(testEnergyAndCostRes.getNumber(), 2, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));
                            }
                            String string=testEnergyAndCostRes.getItemName()+":"+divide;
                            strings.add(string);
                        }
                    }
                    testCompareReportRes.setRemark(strings);
                    map.put("report",testCompareReportRes);
                    return Mono.just(map);
                }else {
                    testCompareReportRes.setAreaName("");
                    testCompareReportRes.setRemark(new ArrayList<>());
                    map.put("report",testCompareReportRes);
                    return Mono.just(map);
                }

            });


    }



    public Mono<HashMap<String,Object>> monthTestCompareExtend(HashMap<String, List<TestEnergyAndCostRes>> map,AnalysitItemReq analysitItemReq){
        HashMap<String, Object> resultMap = new HashMap<>();
        return Flux.fromIterable(map.entrySet())
            .map(Map.Entry::getValue)
            .flatMap(Flux::fromIterable)
            .collectList()
            .flatMap(list->{
                resultMap.put("list",map);
                TestCompareReportRes testCompareReportRes = new TestCompareReportRes();
                ArrayList<TestAnalysisReportRes> reportList = new ArrayList<>();
                TestEnergyAndCostRes max=null;

                List<TestEnergyAndCostRes> testEnergyAndCostRes1 = this.mergeTestCostAndNum(list);
                for (TestEnergyAndCostRes testEnergyAndCostRes : testEnergyAndCostRes1) {
                    if(max==null){
                        max=testEnergyAndCostRes;
                    }else {
                        if(testEnergyAndCostRes.getNumber().compareTo(max.getNumber())>0){
                            max=testEnergyAndCostRes;
                        }
                    }
                    TestAnalysisReportRes testAnalysisReportRes = new TestAnalysisReportRes();
                    testAnalysisReportRes.setTime(analysitItemReq.getStartTime()+"~"+analysitItemReq.getEndTime());
                    testAnalysisReportRes.setAreaName(testEnergyAndCostRes.getItemName());
                    testAnalysisReportRes.setTotal(testEnergyAndCostRes.getNumber().setScale(2,RoundingMode.HALF_UP));
                    testAnalysisReportRes.setCost(testEnergyAndCostRes.getCost().setScale(2,RoundingMode.HALF_UP));
                    reportList.add(testAnalysisReportRes);
                }
                testCompareReportRes.setList(reportList);
                if(max!=null){
                    testCompareReportRes.setAreaName(max.getItemName());
                    ArrayList<String> strings = new ArrayList<>();
                    for (TestEnergyAndCostRes testEnergyAndCostRes : testEnergyAndCostRes1) {
                        BigDecimal number = max.getNumber();
                        if(!testEnergyAndCostRes.equals(max)){
                            BigDecimal divide =BigDecimal.ZERO;
                            if(testEnergyAndCostRes.getNumber().compareTo(BigDecimal.ZERO)==0){
                                divide=BigDecimal.ZERO;
                            }else {
                                divide = number.subtract(testEnergyAndCostRes.getNumber()).divide(testEnergyAndCostRes.getNumber(), 2, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));
                            }

                            String string=testEnergyAndCostRes.getItemName()+":"+divide;
                            strings.add(string);
                        }
                    }
                    testCompareReportRes.setRemark(strings);
                    resultMap.put("report",testCompareReportRes);
                    return Mono.just(resultMap);
                }else {
                    testCompareReportRes.setAreaName("");
                    testCompareReportRes.setRemark(new ArrayList<>());
                    resultMap.put("report",testCompareReportRes);
                    return Mono.just(resultMap);
                }
            });

    }

    public Flux<TestEnergyAndCostRes> monthGetEnergyAndCost(Long startDate,Long endDate,TestEnergyAndCostRes testEnergyAndCostRes){
        //1.获取该段时间内的试验
        return queryHelper.select("SELECT * FROM sems_test_record WHERE not (test_start_time > " +endDate+ " or test_end_time < " + startDate+ ")", TestRecordEntity::new)

            .where(new QueryParamEntity())
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
                        //根据设备统计场所能耗
                        HashMap<String, DateTime> map = testConfigDeviceService.setOverlap(new DateTime(startDate), new DateTime(endDate), new DateTime(record.getTestStartTime()), new DateTime(record.getTestEndTime()));
                        TestEnergyAndCostRes resultEnergy = new TestEnergyAndCostRes();
                        BeanUtils.copyProperties(testEnergyAndCostRes,resultEnergy);
                        resultEnergy.setItemName(record.getTestName());
                                //用量
                                return electricityConsumeService
                                    .createQuery()
                                    .in(ElectricityConsumeEntity::getDeviceId, deviceIds)
                                    .lte(ElectricityConsumeEntity::getGatherTime, map.get("endDate").toDate().getTime())
                                    .gte(ElectricityConsumeEntity::getGatherTime, map.get("startDate").toDate().getTime())
                                    .fetch()
                                    .doOnNext(va -> va.setUnitPrice(va.getDifference().multiply(va.getUnitPrice())))
                                    .collectList()
                                    .flatMap(v -> {
                                        //总能耗
                                        BigDecimal total = v.stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                        //总费用
                                        BigDecimal cost = v.stream().map(ElectricityConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                        resultEnergy.setNumber(total);
                                        resultEnergy.setCost(cost);
                                        return Mono.just(resultEnergy);
                                    });
                            });

            }).doOnNext(va->va.setNum(1))
            .collectList()
            .flatMapMany(li->{
                List<TestEnergyAndCostRes> merge = mergeTest(li);
                return Flux.fromIterable(merge);
            });
    }


    public Flux<TestEnergyAndCostRes> getEnergyAndCost(Long startDate,Long endDate){
        //1.获取该段时间内的试验
        return queryHelper.select("SELECT * FROM sems_test_record WHERE not (test_start_time > " +endDate+ " or test_end_time < " + startDate+ ")", TestRecordEntity::new)

            .where(new QueryParamEntity())
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
                        //根据设备统计场所能耗
                        HashMap<String, DateTime> map = testConfigDeviceService.setOverlap(new DateTime(startDate), new DateTime(endDate), new DateTime(record.getTestStartTime()), new DateTime(record.getTestEndTime()));


                                                    TestEnergyAndCostRes testEnergyAndCostRes = new TestEnergyAndCostRes();
                                                    testEnergyAndCostRes.setItemName(record.getTestName());
                                                    //用量
                                                    return electricityConsumeService
                                                        .createQuery()
                                                        .in(ElectricityConsumeEntity::getDeviceId, deviceIds)
                                                        .lte(ElectricityConsumeEntity::getGatherTime, map.get("endDate").toDate().getTime())
                                                        .gte(ElectricityConsumeEntity::getGatherTime, map.get("startDate").toDate().getTime())
                                                        .fetch()
                                                        .doOnNext(va -> va.setUnitPrice(va.getDifference().multiply(va.getUnitPrice())))
                                                        .collectList()
                                                        .flatMap(v -> {
                                                            //总能耗
                                                            BigDecimal total = v.stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                                                            //总费用
                                                            BigDecimal cost = v.stream().map(ElectricityConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                                            testEnergyAndCostRes.setNumber(total);
                                                            testEnergyAndCostRes.setCost(cost);
                                                            return Mono.just(testEnergyAndCostRes);
                                                        });
                    });

            });
    }

    /**
     * 没有合并相同条目
     * @param startDate
     * @param endDate
     * @param
     * @return
     */
    public Flux<TestEnergyAndCostRes> getEnergyAndCostExtend(Long startDate,Long endDate,TestEnergyAndCostRes testEnergyAndCostRes) {
        //1.获取该段时间内的试验
        return queryHelper.select("SELECT * FROM sems_test_record WHERE not (test_start_time > " + endDate + " or test_end_time < " + startDate + ")", TestRecordEntity::new)

            .where(new QueryParamEntity().noPaging())
            .fetch()
            .flatMap(record -> {
                //根据试验获取该试验对应条目的设备
                return testEnergyDetailService
                    .createQuery()
                    .where(TestEnergyDetailEntity::getTestRecordId, record.getId())
                    .fetch()
                    .map(TestEnergyDetailEntity::getDeviceId)
                    .collectList()
                    .flatMapMany(deviceIds -> {
                        HashMap<String, DateTime> map = testConfigDeviceService.setOverlap(new DateTime(startDate), new DateTime(endDate), new DateTime(record.getTestStartTime()), new DateTime(record.getTestEndTime()));

                        //根据设备统计场所能耗

                        TestEnergyAndCostRes resultEnergy = new TestEnergyAndCostRes();
                        BeanUtils.copyProperties(testEnergyAndCostRes, resultEnergy);
                        resultEnergy.setItemName(record.getTestName());
                        resultEnergy.setTestStartTime(map.get("startDate").toDate().getTime());
                        resultEnergy.setTestEndTime(map.get("endDate").toDate().getTime());
                        //用量
                        return electricityConsumeService
                            .createQuery()
                            .in(ElectricityConsumeEntity::getDeviceId, deviceIds)
                            .lte(ElectricityConsumeEntity::getGatherTime, map.get("endDate").toDate().getTime())
                            .gte(ElectricityConsumeEntity::getGatherTime, map.get("startDate").toDate().getTime())
                            .fetch()
                            .doOnNext(va -> va.setUnitPrice(va.getDifference().multiply(va.getUnitPrice())))
                            .collectList()
                            .flatMap(v -> {
                                //总能耗
                                BigDecimal total = v.stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                                //总费用
                                BigDecimal cost = v.stream().map(ElectricityConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                                resultEnergy.setNumber(total);
                                resultEnergy.setCost(cost);
                                return Mono.just(resultEnergy);
                            });
                    });
            });

    }

    /**
     * 公用设备计算能耗和费用
     * @param unitDevice
     * @param record
     * @param map
     * @return
     */
    public Flux<TestEnergyAndCostRes> allUseDevice(List<TestEnergyDetailEntity> unitDevice,TestRecordEntity record,HashMap<String, DateTime> map,TestEnergyAndCostRes testEnergyAndCostRes ){
        //共用设备
        return energyRatioService
            .createQuery()
            .where(EnergyRatioEntity::getConfigId,record.getConfigId())
            .fetchOne()
            .flatMapMany(ration->{
                return Flux.fromIterable(unitDevice)
                    .flatMap(li->{
                        return deviceService.createQuery()
                            .where(DeviceInfoEntity::getDeviceId,li.getDeviceId())
                            .fetch()
                            .filter(i->i.getPlaceId() != null)
                            .flatMap(deviceInfo->{
                                if(li.getDeviceId().equals("C0505217") || li.getDeviceId().equals("C0505218")){
                                    //普冷
                                    List<EnergyRatioRes> ordinaryFreezing = ration.getOrdinaryFreezing();
                                    EnergyRatioRes energyRatioRes = ordinaryFreezing.stream().filter(i -> i.getPlaceId().equals(deviceInfo.getDeviceId())).findFirst().orElseGet(EnergyRatioRes::new);
                                    //计算能耗
                                    return testAreaService.findById(deviceInfo.getPlaceId())
                                        .flatMap(valie-> {
                                            TestEnergyAndCostRes resultEnergy = new TestEnergyAndCostRes();
                                            BeanUtils.copyProperties(testEnergyAndCostRes, resultEnergy);
                                            resultEnergy.setItemName(record.getTestName());
                                            String reportDevice = null;
                                            if (li.getDeviceId().equals("C0505217")) {
                                                reportDevice = "CWT1-13";
                                            } else {
                                                reportDevice = "CWT1-12";
                                            }
                                            return electricityConsumeService
                                                .createQuery()
                                                .where(ElectricityConsumeEntity::getDeviceId, li.getDeviceId())
                                                .where(ElectricityConsumeEntity::getReportDeviceId, reportDevice)
                                                .lte(ElectricityConsumeEntity::getGatherTime, map.get("endDate").toDate().getTime())
                                                .gte(ElectricityConsumeEntity::getGatherTime, map.get("startDate").toDate().getTime())
                                                .fetch()
                                                .doOnNext(va -> va.setUnitPrice(va.getDifference().multiply(va.getUnitPrice())))
                                                .collectList()
                                                .flatMap(v -> {
                                                    //总能耗
                                                    BigDecimal total = v.stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                                    //总费用
                                                    BigDecimal cost = v.stream().map(ElectricityConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                                    BigDecimal multiply = total.multiply(energyRatioRes.getEnergyRadio());
                                                    BigDecimal costMutiply = cost.multiply(energyRatioRes.getEnergyRadio());
                                                    resultEnergy.setNumber(multiply);
                                                    resultEnergy.setCost(costMutiply);
                                                    return Mono.just(resultEnergy);
                                                })
                                                .thenReturn(resultEnergy);
                                        });
                                }else if(li.getDeviceId().equals("C0801008") ){
                                    //组合开始冷却塔（自动）
                                    List<EnergyRatioRes> ordinaryFreezingAuto = ration.getCombinedCoolingTowerAuto();
                                    EnergyRatioRes energyRatioResAuto = ordinaryFreezingAuto.stream().filter(i -> i.getPlaceId().equals(deviceInfo.getDeviceId())).findFirst().orElseGet(EnergyRatioRes::new);
                                    //组合开始冷却塔（手动）
                                    List<EnergyRatioRes> ordinaryFreezing = ration.getCombinedCoolingTower();
                                    EnergyRatioRes energyRatioRes = ordinaryFreezing.stream().filter(i -> i.getPlaceId().equals(deviceInfo.getDeviceId())).findFirst().orElseGet(EnergyRatioRes::new);
                                    //计算能耗
                                    return testAreaService.findById(deviceInfo.getPlaceId())
                                        .flatMap(valie-> {
                                            TestEnergyAndCostRes resultEnergy = new TestEnergyAndCostRes();
                                            BeanUtils.copyProperties(testEnergyAndCostRes, resultEnergy);
                                            resultEnergy.setItemName(record.getTestName());

                                            return electricityConsumeService
                                                .createQuery()
                                                .where(ElectricityConsumeEntity::getDeviceId, li.getDeviceId())
                                                .where(ElectricityConsumeEntity::getReportDeviceId, "CWT2-14")
                                                .lte(ElectricityConsumeEntity::getGatherTime, map.get("endDate").toDate().getTime())
                                                .gte(ElectricityConsumeEntity::getGatherTime, map.get("startDate").toDate().getTime())
                                                .fetch()
                                                .doOnNext(va -> va.setUnitPrice(va.getDifference().multiply(va.getUnitPrice())))
                                                .collectList()
                                                .flatMap(v -> {
                                                    //区分手动还是自动
                                                    Map<String, List<ElectricityConsumeEntity>> collect = v.stream().collect(Collectors.groupingBy(ElectricityConsumeEntity::getDeviceRunStatus));
                                                    BigDecimal totalEnergy=BigDecimal.ZERO;
                                                    BigDecimal totalCost=BigDecimal.ZERO;
                                                    //自动
                                                    if(collect.get("1")!= null){
                                                        //能耗
                                                        BigDecimal total=collect.get("1").stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                                        //费用
                                                        BigDecimal cost = collect.get("1").stream().map(ElectricityConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                                        BigDecimal multiply = total.multiply(energyRatioResAuto.getEnergyRadio());
                                                        BigDecimal costMutiply = cost.multiply(energyRatioResAuto.getEnergyRadio());
                                                        totalEnergy=totalEnergy.add(multiply);
                                                        totalCost=totalCost.add(costMutiply);
                                                    }
                                                    //手动
                                                    if(collect.get("2")!= null){
                                                        //能耗
                                                        BigDecimal total=collect.get("2").stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                                        //费用
                                                        BigDecimal cost = collect.get("2").stream().map(ElectricityConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                                        BigDecimal multiply = total.multiply(energyRatioRes.getEnergyRadio());
                                                        BigDecimal costMutiply = cost.multiply(energyRatioRes.getEnergyRadio());
                                                        totalEnergy=totalEnergy.add(multiply);
                                                        totalCost=totalCost.add(costMutiply);
                                                    }
                                                    resultEnergy.setNumber(totalEnergy);
                                                    resultEnergy.setCost(totalCost);
                                                    return Mono.just(resultEnergy);
                                                })
                                                .thenReturn(resultEnergy);
                                        });
                                }else if(li.getDeviceId().equals("C0507049")) {
                                    //锅炉
                                    List<EnergyRatioRes> ordinaryFreezing = ration.getBoiler();
                                    EnergyRatioRes energyRatioRes = ordinaryFreezing.stream().filter(i -> i.getPlaceId().equals(deviceInfo.getDeviceId())).findFirst().orElseGet(EnergyRatioRes::new);
                                    //计算能耗
                                    return testAreaService.findById(deviceInfo.getPlaceId())
                                        .flatMap(valie-> {
                                            TestEnergyAndCostRes resultEnergy = new TestEnergyAndCostRes();
                                            BeanUtils.copyProperties(testEnergyAndCostRes, resultEnergy);
                                            resultEnergy.setItemName(record.getTestName());

                                            return electricityConsumeService
                                                .createQuery()
                                                .where(ElectricityConsumeEntity::getDeviceId, li.getDeviceId())
                                                .where(ElectricityConsumeEntity::getReportDeviceId, "CWT2-5")
                                                .lte(ElectricityConsumeEntity::getGatherTime, map.get("endDate").toDate().getTime())
                                                .gte(ElectricityConsumeEntity::getGatherTime, map.get("startDate").toDate().getTime())
                                                .fetch()
                                                .doOnNext(va -> va.setUnitPrice(va.getDifference().multiply(va.getUnitPrice())))
                                                .collectList()
                                                .flatMap(v -> {
                                                    //总能耗
                                                    BigDecimal total = v.stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                                    //总费用
                                                    BigDecimal cost = v.stream().map(ElectricityConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                                    BigDecimal multiply = total.multiply(energyRatioRes.getEnergyRadio());
                                                    BigDecimal costMutiply = cost.multiply(energyRatioRes.getEnergyRadio());
                                                    resultEnergy.setNumber(multiply);
                                                    resultEnergy.setCost(costMutiply);
                                                    return Mono.just(resultEnergy);
                                                })
                                                .thenReturn(resultEnergy);
                                        });
                                }else if(li.getDeviceId().equals("A0401056")) {
                                    //空压冷却塔（现在还没有气动和声学）
                                    List<EnergyRatioRes> ordinaryFreezing = ration.getAirCompressor();
                                    EnergyRatioRes energyRatioRes = ordinaryFreezing.stream().filter(i -> i.getPlaceId().equals(deviceInfo.getDeviceId())).findFirst().orElseGet(EnergyRatioRes::new);
                                    //计算能耗
                                    return testAreaService.findById(deviceInfo.getPlaceId())
                                        .flatMap(valie -> {
                                            TestEnergyAndCostRes resultEnergy = new TestEnergyAndCostRes();
                                            BeanUtils.copyProperties(testEnergyAndCostRes, resultEnergy);
                                            resultEnergy.setItemName(record.getTestName());

                                            return electricityConsumeService
                                                .createQuery()
                                                .where(ElectricityConsumeEntity::getDeviceId, li.getDeviceId())
                                                .where(ElectricityConsumeEntity::getReportDeviceId, "AAWT2-9")
                                                .lte(ElectricityConsumeEntity::getGatherTime, map.get("endDate").toDate().getTime())
                                                .gte(ElectricityConsumeEntity::getGatherTime, map.get("startDate").toDate().getTime())
                                                .fetch()
                                                .doOnNext(va -> va.setUnitPrice(va.getDifference().multiply(va.getUnitPrice())))
                                                .collectList()
                                                .flatMap(v -> {
                                                    //总能耗
                                                    BigDecimal total = v.stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                                    //总费用
                                                    BigDecimal cost = v.stream().map(ElectricityConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                                    BigDecimal multiply = total.multiply(energyRatioRes.getEnergyRadio());
                                                    BigDecimal costMutiply = cost.multiply(energyRatioRes.getEnergyRadio());
                                                    resultEnergy.setNumber(multiply);
                                                    resultEnergy.setCost(costMutiply);
                                                    return Mono.just(resultEnergy);
                                                })
                                                .thenReturn(resultEnergy);
                                        });
                                }else {
                                    return testAreaService.findById(deviceInfo.getPlaceId())
                                        .flatMap(valie-> {
                                            TestEnergyAndCostRes resultEnergy = new TestEnergyAndCostRes();
                                            BeanUtils.copyProperties(testEnergyAndCostRes, resultEnergy);
                                            resultEnergy.setItemName(record.getTestName());
                                            return electricityConsumeService
                                                .createQuery()
                                                .where(ElectricityConsumeEntity::getDeviceId, li.getDeviceId())
                                                .lte(ElectricityConsumeEntity::getGatherTime, map.get("endDate").toDate().getTime())
                                                .gte(ElectricityConsumeEntity::getGatherTime, map.get("startDate").toDate().getTime())
                                                .fetch()
                                                .doOnNext(va -> va.setUnitPrice(va.getDifference().multiply(va.getUnitPrice())))
                                                .collectList()
                                                .flatMap(v -> {
                                                    //总能耗
                                                    BigDecimal total = v.stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                                    //总费用
                                                    BigDecimal cost = v.stream().map(ElectricityConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
                                                    resultEnergy.setNumber(total);
                                                    resultEnergy.setCost(cost);
                                                    return Mono.just(resultEnergy);
                                                })
                                                .thenReturn(resultEnergy);
                                        });
                                }
                            });
                    });
            }).collectList()
            .flatMapMany(va-> {return Flux.fromIterable(this.mergeTest(va));
            });
    }

    //组合数据
    public List<TestEnergyAndCostRes> mergeTestCostAndNum(List<TestEnergyAndCostRes> list) {

        List<TestEnergyAndCostRes> result = list.stream()
            // 表示name为key， 接着如果有重复的，那么从Singer对象o1与o2中筛选出一个，这里选择o1，
            // 并把name重复，需要将ages与o1进行合并的o2, 赋值给o1，最后返回o1
            .collect(Collectors.toMap(TestEnergyAndCostRes::getItemName, a -> a, (o1, o2) -> {
                TestEnergyAndCostRes testEnergyAndCostRes = new TestEnergyAndCostRes();
                testEnergyAndCostRes.setItemName(o1.getItemName());
                testEnergyAndCostRes.setNumber(o1.getNumber().add(o2.getNumber()));
                testEnergyAndCostRes.setCost(o1.getCost().add(o2.getCost()));
                return testEnergyAndCostRes;
            })).values().stream().collect(Collectors.toList());
        return result;

    }

    //组合数据
    public List<TestEnergyAndCostRes> mergeTest(List<TestEnergyAndCostRes> list) {
        List<TestEnergyAndCostRes> result = list.stream()
            // 表示name为key， 接着如果有重复的，那么从Singer对象o1与o2中筛选出一个，这里选择o1，
            // 并把name重复，需要将ages与o1进行合并的o2, 赋值给o1，最后返回o1
            .collect(Collectors.toMap(TestEnergyAndCostRes::getItemName, a -> a, (o1, o2) -> {
                o1.setNumber(o1.getNumber().add(o2.getNumber()));
                o1.setCost(o1.getCost().add(o2.getCost()));
                o1.setNum(o1.getNum()+1);
                return o1;
            })).values().stream().collect(Collectors.toList());
        return result;

    }

    /**
     * 根据时间段获取时间轴
     * @param startDate
     * @param endDate
     * @return
     */
    public List<TestEnergyAndCostRes> getTimeByDate(Date startDate,Date endDate,int day){
        ArrayList<TestEnergyAndCostRes> result = new ArrayList<>();
      if(day<=31){
            Date useStart=startDate;
            while (useStart.compareTo(endDate)<=0){
                TestEnergyAndCostRes testEnergyAndCostRes = new TestEnergyAndCostRes();
                String date = DateUtil.dateToString(useStart, DateUtil.DATE_SHORT_FORMAT);
                testEnergyAndCostRes.setTime(date);
                testEnergyAndCostRes.setTimeStart(DateUtil.stringToDate(date+" 00:00:00",DateUtil.DATE_WITHSECOND_FORMAT).getTime());
                testEnergyAndCostRes.setTimeEnd(DateUtil.stringToDate(date+" 23:59:59",DateUtil.DATE_WITHSECOND_FORMAT).getTime());
                useStart=DateUtil.addDays(useStart,1);
                result.add(testEnergyAndCostRes);
            }
        }else if(day<=365){
            Date useStart=startDate;
            while (useStart.compareTo(endDate)<=0){
                TestEnergyAndCostRes testEnergyAndCostRes = new TestEnergyAndCostRes();
                String dateMonth = DateUtil.dateToString(useStart, DateUtil.DATE_SHORT_YEAR_MONTH);
                testEnergyAndCostRes.setTime(dateMonth);
                testEnergyAndCostRes.setTimeStart(DateUtil.getFirstDateOfMonth(useStart).getTime());
                testEnergyAndCostRes.setTimeEnd(DateUtil.getLastDateOfMonth(useStart).getTime());
                useStart=DateUtil.addMonths(useStart,1);
                result.add(testEnergyAndCostRes);
            }
        }
      return result;

    }



}
