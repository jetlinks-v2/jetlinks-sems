package org.jetlinks.project.busi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.DeviceInfoEntity;
import org.jetlinks.project.busi.entity.ElectricityConsumeEntity;
import org.jetlinks.project.busi.entity.res.*;
import org.jetlinks.project.busi.service.DeviceService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sems/device/energy/monitor")
@AllArgsConstructor
@Getter
@Tag(name = "设备能耗监测1.0") //swagger
@Slf4j
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "device-energy-monitor", name = "设备能耗监测")
public class DeviceEnergyMonitorController {

    private final QueryHelper queryHelper;

    private final DeviceService deviceService;

    @Operation(summary = "设备数量")
    @GetMapping("/device/number")
    @QueryAction
    public Mono<DeviceNumberRes> getDeviceNumber() {
        Long startTime = LocalDate.now().withDayOfYear(1).atStartOfDay().toInstant(ZoneOffset.of("+8")).toEpochMilli();
        Long endTime = System.currentTimeMillis();
        return queryHelper.select("SELECT \n" +
                                      "COUNT(id) as deviceTotal,\n" +
                                      "COUNT(if(compute_status = '3',compute_status,null)) as runNumber,\n" +
                                      "COUNT(if(compute_status = '4',compute_status,null)) as stopNumber\n" +
                                      "FROM sems_device_info \n" +
                                      "WHERE `status` = '0' AND parent_id = '0' AND place_id != '0'",DeviceNumberRes::new)
                          .fetch()
                          .collectList()
                          .flatMap(e -> {
                              DeviceNumberRes res = new DeviceNumberRes();
                              res.setDeviceTotal(e.get(0).getDeviceTotal());
                              res.setRunNumber(e.get(0).getRunNumber());
                              res.setStopNumber(e.get(0).getStopNumber());
                              return queryHelper
                                  .select("SELECT \n" +
                                              "IFNULL(SUM(difference),0) as energySum\n" +
                                              "FROM sems_electricity_consume\n" +
                                              "WHERE gather_time BETWEEN ? AND ? AND device_id != '0'"
                                      ,DeviceNumberRes::new,startTime,endTime)
                                  .fetch()
                                  .collectList()
                                  .flatMap(list ->{
                                      res.setEnergySum(list.get(0).getEnergySum());
                                      return Mono.just(res);
                                  });
                          });

    }

    @Operation(summary = "查询设备信息")
    @GetMapping("/monitor/info")
    @QueryAction
    public Mono<List<DeviceMonitorInfoRes>> getMonitorInfo(String placeIds) {
        String[] placeId;
        if(StringUtils.isNotEmpty(placeIds) && placeIds.contains(",")){
            placeId = placeIds.split(",");
            return queryHelper
                .select("SELECT\n" +
                            "di.device_id as deviceId,\n" +
                            "di.device_name as deviceName\n" +
                            "FROM\n" +
                            "sems_device_info di\n" +
                            "WHERE\n" +
                            "di.`status` = \"0\""
                    ,DeviceMonitorInfoRes::new)
                .where(dsl->dsl.in("di.place_id",placeId))
                .fetch()
                .flatMap(res -> getDeviceMonthEnergy(res)
                    .switchIfEmpty(Mono.just(BigDecimal.ZERO))
                    .flatMap(energySum -> {
                        res.setEnergySum(energySum);
                        return Mono.just(res);
                    })
                )
                .flatMap(res -> getDeviceMonthRunTime(res)
                    .switchIfEmpty(Mono.just(BigDecimal.ZERO))
                    .flatMap(runHour -> {
                        res.setRunHour(runHour);
                        return Mono.just(res);
                    })
                )
                .collectList()
                .flatMap(list -> {
                    List<DeviceMonitorInfoRes> sortList;
                    sortList = list.stream().sorted(Comparator.comparing(DeviceMonitorInfoRes::getDeviceName).reversed()).collect(Collectors.toList());
                    return Mono.just(sortList);
                });
        }else {
            return queryHelper.select("SELECT\n" +
                                          "di.device_id as deviceId,\n" +
                                          "di.device_name as deviceName\n" +
                                          "FROM\n" +
                                          "sems_device_info di\n" +
                                          "WHERE\n" +
                                          "di.`status` = \"0\"",DeviceMonitorInfoRes::new)
                .where(dsl->dsl.in("di.place_id",placeIds))
                .fetch()
                .flatMap(res -> getDeviceMonthEnergy(res)
                    .switchIfEmpty(Mono.just(BigDecimal.ZERO))
                    .flatMap(energySum -> {
                        res.setEnergySum(energySum);
                        return Mono.just(res);
                    })
                )
                .flatMap(res -> getDeviceMonthRunTime(res)
                    .switchIfEmpty(Mono.just(BigDecimal.ZERO))
                    .flatMap(runHour -> {
                        res.setRunHour(runHour);
                        return Mono.just(res);
                    })
                )
                .collectList()
                .flatMap(list -> {
                     List<DeviceMonitorInfoRes> sortList;
                     sortList = list.stream().sorted(Comparator.comparing(DeviceMonitorInfoRes::getDeviceName).reversed()).collect(Collectors.toList());
                     return Mono.just(sortList);
                });
        }

    }

    @Operation(summary = "查询年度设备时长")
    @GetMapping("/device/run/time")
    @QueryAction
    public Mono<Object> getDeviceRunTime(Integer year) {
        return queryHelper
            .select("SELECT \n" +
                        "FROM_UNIXTIME(SUBSTR(start_time,1,10),'%Y-%m') as gatherTime,\n" +
                        "IFNULL(sum(end_time - start_time)/3600000,0) as difference\n" +
                        "FROM `sems_device_state` \n" +
                        "WHERE\n" +
                        "( YEAR(FROM_UNIXTIME(SUBSTR(start_time,1,10))) = ?\n" +
                        "OR YEAR(FROM_UNIXTIME(SUBSTR(end_time,1,10))) = ? )\n" +
                        "GROUP BY FROM_UNIXTIME(SUBSTR(start_time,1,10),'%Y-%m')", EnergyDayRes::new,year,year)
            .fetch()
            .collectMap(EnergyDayRes::getGatherTime, EnergyDayRes::getDifference)
            .flatMap(resMap->{
                YearMonth startTime = YearMonth.of(year, 1);
                YearMonth endTime = YearMonth.of(year, 12);
                List<EnergyDayRes> list = new ArrayList<>();
                do {
                    EnergyDayRes dayRes = new EnergyDayRes();
                    dayRes.setGatherTime(startTime.toString());
                    dayRes.setDifference(BigDecimal.ZERO);
                    list.add(dayRes);
                    startTime = startTime.plusMonths(1);
                } while (startTime.compareTo(endTime) <= 0);

                for (EnergyDayRes dayRes:list) {
                    if(resMap.containsKey(dayRes.getGatherTime())){
                        dayRes.setDifference(resMap.get(dayRes.getGatherTime()));
                    }
                }
                return Mono.just(list);
            });
    }

    //查询设备当前月运行时长
    public Mono<BigDecimal> getDeviceMonthRunTime(DeviceMonitorInfoRes res) {
        return queryHelper
            .select("SELECT \n" +
                        "(end_time - start_time)/3600000 as difference\n" +
                        "FROM `sems_device_state` \n" +
                        "WHERE device_id = ? \n" +
                        "AND ( FROM_UNIXTIME(SUBSTR(start_time,1,10),'%Y-%m') = DATE_FORMAT(CURRENT_DATE,'%Y-%m')\n" +
                        "OR FROM_UNIXTIME(SUBSTR(end_time,1,10),'%Y-%m') = DATE_FORMAT(CURRENT_DATE,'%Y-%m') )",
                    EnergyDayRes::new,res.getDeviceId())
            .fetch()
            .flatMap(value->{
                if(value.getDifference()==null){
                    return Mono.empty();
                }
                return Mono.just(value);
            })
            .switchIfEmpty(deviceService.createQuery()
                                        .where(DeviceInfoEntity::getDeviceId,res.getDeviceId())
                                        .and(DeviceInfoEntity::getStatus,"0")
                                        .fetchOne()
                                        .flatMap(device ->{
                                            if(("3").equals(device.getComputeStatus())){
                                                EnergyDayRes dayRes = new EnergyDayRes();
                                                long startTime = LocalDateTime.of(LocalDate.of(LocalDate.now().getYear(),
                                                                                               LocalDate.now().getMonthValue(), 1),
                                                                                  LocalTime.MIN)
                                                                              .toInstant(ZoneOffset.of("+8"))
                                                                              .toEpochMilli();
                                                dayRes.setDifference(BigDecimal.valueOf((System.currentTimeMillis()-startTime)/3600000));
                                                return Mono.just(dayRes);
                                            }
                                            return Mono.empty();
                                        }))
            .mapNotNull(EnergyDayRes::getDifference)
            .reduce(BigDecimal::add);
    }

    //查询设备当前月能耗
    public Mono<BigDecimal> getDeviceMonthEnergy(DeviceMonitorInfoRes res) {
        return queryHelper
            .select("SELECT\n" +
                        "SUM(difference) AS difference \n" +
                        "FROM\n" +
                        "sems_electricity_consume\n" +
                        "WHERE device_id = ? \n" +
                        "AND FROM_UNIXTIME(SUBSTR(gather_time,1,10), '%Y-%m' ) = DATE_FORMAT(CURRENT_DATE,'%Y-%m')",
                    EnergyDayRes::new,res.getDeviceId())
            .fetch()
            .mapNotNull(EnergyDayRes::getDifference)
            .reduce(BigDecimal::add);
    }

    @Operation(summary = "设备状态")
    @GetMapping("/device/current/status")
    @QueryAction
    public Mono<DeviceMonitorStatusRes> queryDeviceCurrentInfo(String deviceId,Integer year){
        return queryHelper
            .select("SELECT \n" +
                        "* \n" +
                        "FROM \n" +
                        "sems_electricity_consume \n" +
                        "ORDER BY gather_time DESC",DeviceMonitorStatusRes::new)
            .where(dsl -> dsl.and(ElectricityConsumeEntity::getDeviceId,deviceId)
                             .doPaging(0,1))
            .fetch()
            .collectList()
            .flatMap(list -> Mono.just(list.get(0)))
            .flatMap(e -> queryHelper
                .select("SELECT \n" +
                            "FROM_UNIXTIME(SUBSTR( gather_time,1,10),'%Y-%m') as gatherTime,\n" +
                            "sum(difference) AS `difference` \n" +
                            "FROM `sems_electricity_consume` \n" +
                            "WHERE device_id = ? AND\n" +
                            "YEAR(FROM_UNIXTIME(SUBSTR(gather_time,1,10))) = ?\n" +
                            "GROUP BY FROM_UNIXTIME(SUBSTR(gather_time,1,10),'%Y-%m')", EnergyDayRes::new,deviceId,year,year)
                .fetch()
                .collectMap(EnergyDayRes::getGatherTime, EnergyDayRes::getDifference)
                .flatMap(resMap->{
                    YearMonth startTime = YearMonth.of(year, 1);
                    YearMonth endTime = YearMonth.of(year, 12);
                    List<EnergyDayRes> list = new ArrayList<>();
                    do {
                        EnergyDayRes dayRes = new EnergyDayRes();
                        dayRes.setGatherTime(startTime.toString());
                        dayRes.setDifference(BigDecimal.ZERO);
                        list.add(dayRes);
                        startTime = startTime.plusMonths(1);
                    } while (startTime.compareTo(endTime) <= 0);

                    for (EnergyDayRes dayRes:list) {
                        if(resMap.containsKey(dayRes.getGatherTime())){
                            dayRes.setDifference(resMap.get(dayRes.getGatherTime()));
                        }
                    }
                    e.setYearEnergyList(list);
                    return Mono.just(e);
                }));
    }

    @Operation(summary = "今日实时电流")
    @GetMapping("/device/today/current")
    @QueryAction
    public Mono<DeviceMonitorStatusRes> getDeviceTodayCurrent(String deviceId){
        DeviceMonitorStatusRes res = new DeviceMonitorStatusRes();
        return queryHelper
                .select("SELECT\n" +
                            "gather_time as gatherTime,\n" +
                            "phase_i_a as difference\n" +
                            "FROM\n" +
                            "sems.`sems_electricity_consume` \n" +
                            "WHERE device_id = ? AND\n" +
                            "DATE(FROM_UNIXTIME(SUBSTR(gather_time,1,10))) = DATE(NOW())", EnergyDayRes::new, deviceId)
                .fetch()
                .collectList()
                .flatMap(phaseIA ->{
                    EnergyConserveRes phaseIARes = new EnergyConserveRes();
                    phaseIARes.setEnergyList(phaseIA);
                    phaseIARes.setName("A相电流");
                    res.setPhaseIA(phaseIARes);

                    return Mono.just(res);
                })
                .flatMap(e-> queryHelper
                    .select("SELECT\n" +
                                "gather_time as gatherTime,\n" +
                                "phase_i_b as difference\n" +
                                "FROM\n" +
                                "sems.`sems_electricity_consume` \n" +
                                "WHERE device_id = ? AND\n" +
                                "DATE(FROM_UNIXTIME(SUBSTR(gather_time,1,10))) = DATE(NOW())", EnergyDayRes::new, deviceId)
                    .fetch()
                    .collectList()
                    .flatMap(phaseIB ->{
                        EnergyConserveRes phaseIBRes = new EnergyConserveRes();
                        phaseIBRes.setEnergyList(phaseIB);
                        phaseIBRes.setName("B相电流");
                        res.setPhaseIB(phaseIBRes);

                        return Mono.just(res);
                    }))
                .flatMap(e-> queryHelper
                    .select("SELECT\n" +
                                "gather_time as gatherTime,\n" +
                                "phase_i_c as difference\n" +
                                "FROM\n" +
                                "sems.`sems_electricity_consume` \n" +
                                "WHERE device_id = ? AND\n" +
                                "DATE(FROM_UNIXTIME(SUBSTR(gather_time,1,10))) = DATE(NOW())", EnergyDayRes::new, deviceId)
                    .fetch()
                    .collectList()
                    .flatMap(phaseIC ->{
                        EnergyConserveRes phaseICRes = new EnergyConserveRes();
                        phaseICRes.setEnergyList(phaseIC);
                        phaseICRes.setName("C相电流");
                        res.setPhaseIC(phaseICRes);

                        return Mono.just(res);
                    }));
    }

    @Operation(summary = "今日实时电压")
    @GetMapping("/device/today/voltage")
    @QueryAction
    public Mono<DeviceMonitorStatusRes> getDeviceTodayVoltage(String deviceId){
        DeviceMonitorStatusRes res = new DeviceMonitorStatusRes();
        return queryHelper
            .select("SELECT\n" +
                        "gather_time as gatherTime,\n" +
                        "phase_u_a as difference\n" +
                        "FROM\n" +
                        "sems.`sems_electricity_consume` \n" +
                        "WHERE device_id = ? AND\n" +
                        "DATE(FROM_UNIXTIME(SUBSTR(gather_time,1,10))) = DATE(NOW())", EnergyDayRes::new, deviceId)
            .fetch()
            .collectList()
            .flatMap(phaseUA ->{
                EnergyConserveRes phaseUARes = new EnergyConserveRes();
                phaseUARes.setEnergyList(phaseUA);
                phaseUARes.setName("A相电压");
                res.setPhaseUA(phaseUARes);

                return Mono.just(res);
            })
            .flatMap(e-> queryHelper
                .select("SELECT\n" +
                            "gather_time as gatherTime,\n" +
                            "phase_u_b as difference\n" +
                            "FROM\n" +
                            "sems.`sems_electricity_consume` \n" +
                            "WHERE device_id = ? AND\n" +
                            "DATE(FROM_UNIXTIME(SUBSTR(gather_time,1,10))) = DATE(NOW())", EnergyDayRes::new, deviceId)
                .fetch()
                .collectList()
                .flatMap(phaseUB ->{
                    EnergyConserveRes phaseUBRes = new EnergyConserveRes();
                    phaseUBRes.setEnergyList(phaseUB);
                    phaseUBRes.setName("B相电压");
                    res.setPhaseUB(phaseUBRes);

                    return Mono.just(res);
                })
            )
            .flatMap(e-> queryHelper
                .select("SELECT\n" +
                            "gather_time as gatherTime,\n" +
                            "phase_u_c as difference\n" +
                            "FROM\n" +
                            "sems.`sems_electricity_consume` \n" +
                            "WHERE device_id = ? AND\n" +
                            "DATE(FROM_UNIXTIME(SUBSTR(gather_time,1,10))) = DATE(NOW())", EnergyDayRes::new, deviceId)
                .fetch()
                .collectList()
                .flatMap(phaseUC ->{
                    EnergyConserveRes phaseUCRes = new EnergyConserveRes();
                    phaseUCRes.setEnergyList(phaseUC);
                    phaseUCRes.setName("C相电压");
                    res.setPhaseUC(phaseUCRes);

                    return Mono.just(res);
                })
            );
    }

    @Operation(summary = "今日有功功率")
    @GetMapping("/device/today/activePower")
    @QueryAction
    public Mono<DeviceMonitorStatusRes> getDeviceTodayActivePower(String deviceId) {
        DeviceMonitorStatusRes res = new DeviceMonitorStatusRes();
        return queryHelper
            .select("SELECT\n" +
                        "gather_time as gatherTime,\n" +
                        "active_p_a as difference\n" +
                        "FROM\n" +
                        "sems.`sems_electricity_consume` \n" +
                        "WHERE device_id = ? AND\n" +
                        "DATE(FROM_UNIXTIME(SUBSTR(gather_time,1,10))) = DATE(NOW())", EnergyDayRes::new, deviceId)
            .fetch()
            .collectList()
            .flatMap(activePA ->{
                EnergyConserveRes activePARes = new EnergyConserveRes();
                activePARes.setEnergyList(activePA);
                activePARes.setName("A相有功功率");
                res.setActivePA(activePARes);

                return Mono.just(res);
            })
            .flatMap(e-> queryHelper
                .select("SELECT\n" +
                            "gather_time as gatherTime,\n" +
                            "active_p_b as difference\n" +
                            "FROM\n" +
                            "sems.`sems_electricity_consume` \n" +
                            "WHERE device_id = ? AND\n" +
                            "DATE(FROM_UNIXTIME(SUBSTR(gather_time,1,10))) = DATE(NOW())", EnergyDayRes::new, deviceId)
                .fetch()
                .collectList()
                .flatMap(activePB ->{
                    EnergyConserveRes activePBRes = new EnergyConserveRes();
                    activePBRes.setEnergyList(activePB);
                    activePBRes.setName("B相有功功率");
                    res.setActivePB(activePBRes);

                    return Mono.just(res);
                })
            )
            .flatMap(e-> queryHelper
                .select("SELECT\n" +
                            "gather_time as gatherTime,\n" +
                            "active_p_c as difference\n" +
                            "FROM\n" +
                            "sems.`sems_electricity_consume` \n" +
                            "WHERE device_id = ? AND\n" +
                            "DATE(FROM_UNIXTIME(SUBSTR(gather_time,1,10))) = DATE(NOW())", EnergyDayRes::new, deviceId)
                .fetch()
                .collectList()
                .flatMap(activePC ->{
                    EnergyConserveRes activePCRes = new EnergyConserveRes();
                    activePCRes.setEnergyList(activePC);
                    activePCRes.setName("C相有功功率");
                    res.setActivePC(activePCRes);

                    return Mono.just(res);
                })
            )
            .flatMap(e-> queryHelper
                .select("SELECT\n" +
                            "gather_time as gatherTime,\n" +
                            "power as difference\n" +
                            "FROM\n" +
                            "sems.`sems_electricity_consume` \n" +
                            "WHERE device_id = ? AND\n" +
                            "DATE(FROM_UNIXTIME(SUBSTR(gather_time,1,10))) = DATE(NOW())", EnergyDayRes::new, deviceId)
                .fetch()
                .collectList()
                .flatMap(power ->{
                    EnergyConserveRes powerRes = new EnergyConserveRes();
                    powerRes.setEnergyList(power);
                    powerRes.setName("总有功功率");
                    res.setPower(powerRes);

                    return Mono.just(res);
                })
            );
    }

    @Operation(summary = "今日有功功率")
    @GetMapping("/device/today/reactivePower")
    @QueryAction
    public Mono<DeviceMonitorStatusRes> getDeviceTodayReactivePower(String deviceId) {
        DeviceMonitorStatusRes res = new DeviceMonitorStatusRes();
        return queryHelper
            .select("SELECT\n" +
                        "gather_time as gatherTime,\n" +
                        "reactive_p_a as difference\n" +
                        "FROM\n" +
                        "sems.`sems_electricity_consume` \n" +
                        "WHERE device_id = ? AND\n" +
                        "DATE(FROM_UNIXTIME(SUBSTR(gather_time,1,10))) = DATE(NOW())", EnergyDayRes::new, deviceId)
            .fetch()
            .collectList()
            .flatMap(reactivePA ->{
                EnergyConserveRes reactivePARes = new EnergyConserveRes();
                reactivePARes.setEnergyList(reactivePA);
                reactivePARes.setName("A相无功功率");
                res.setReactivePA(reactivePARes);

                return Mono.just(res);
            })
            .flatMap(e-> queryHelper
                .select("SELECT\n" +
                            "gather_time as gatherTime,\n" +
                            "reactive_p_b as difference\n" +
                            "FROM\n" +
                            "sems.`sems_electricity_consume` \n" +
                            "WHERE device_id = ? AND\n" +
                            "DATE(FROM_UNIXTIME(SUBSTR(gather_time,1,10))) = DATE(NOW())", EnergyDayRes::new, deviceId)
                .fetch()
                .collectList()
                .flatMap(reactivePB ->{
                    EnergyConserveRes reactivePBRes = new EnergyConserveRes();
                    reactivePBRes.setEnergyList(reactivePB);
                    reactivePBRes.setName("B相无功功率");
                    res.setReactivePB(reactivePBRes);

                    return Mono.just(res);
                })
            )
            .flatMap(e-> queryHelper
                .select("SELECT\n" +
                            "gather_time as gatherTime,\n" +
                            "reactive_p_c as difference\n" +
                            "FROM\n" +
                            "sems.`sems_electricity_consume` \n" +
                            "WHERE device_id = ? AND\n" +
                            "DATE(FROM_UNIXTIME(SUBSTR(gather_time,1,10))) = DATE(NOW())", EnergyDayRes::new, deviceId)
                .fetch()
                .collectList()
                .flatMap(reactivePC ->{
                    EnergyConserveRes reactivePCRes = new EnergyConserveRes();
                    reactivePCRes.setEnergyList(reactivePC);
                    reactivePCRes.setName("C相无功功率");
                    res.setReactivePC(reactivePCRes);

                    return Mono.just(res);
                })
            )
            .flatMap(e-> queryHelper
                .select("SELECT\n" +
                            "gather_time as gatherTime,\n" +
                            "reactive_p_total as difference\n" +
                            "FROM\n" +
                            "sems.`sems_electricity_consume` \n" +
                            "WHERE device_id = ? AND\n" +
                            "DATE(FROM_UNIXTIME(SUBSTR(gather_time,1,10))) = DATE(NOW())", EnergyDayRes::new, deviceId)
                .fetch()
                .collectList()
                .flatMap(reactivePTotal ->{
                    EnergyConserveRes reactivePTotalRes = new EnergyConserveRes();
                    reactivePTotalRes.setEnergyList(reactivePTotal);
                    reactivePTotalRes.setName("总无功功率");
                    res.setReactivePTotal(reactivePTotalRes);

                    return Mono.just(res);
                }));
    }

}
