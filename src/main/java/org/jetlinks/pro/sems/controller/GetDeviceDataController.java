package org.jetlinks.pro.sems.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.sems.entity.req.GetIotDeviceEnergy;
import org.jetlinks.pro.sems.service.*;
import org.jetlinks.pro.sems.service.event.EnergyGatherEventHandler;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.jetlinks.pro.sems.entity.DeviceInfoEntity;
import org.jetlinks.pro.sems.entity.ElectricityConsumeEntity;
import org.jetlinks.pro.sems.entity.GasConsumeEntity;
import org.jetlinks.pro.sems.entity.WaterConsumeEntity;
import org.jetlinks.pro.sems.enums.EnergyType;
//import org.jetlinks.project.busi.iot.IotService;
import org.jetlinks.pro.sems.iot.IotService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/sems/update/_data")
@AllArgsConstructor
@Getter
@Tag(name = "更新水电气表数据") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "update_data", name = "更新数据")
public class GetDeviceDataController {

//    private final IotService iotService;

    private final IotAccessService iotAccessService;

    private final EnergyGatherEventHandler energyGatherEventHandler;

    private final DeviceService deviceService;

    private final ElectricityConsumeService electricityConsumeService;

    private final WaterConsumeService waterConsumeService;

    private final GasConsumeService gasConsumeService;

    private final IotService iotService;

    @Operation(summary = "获取水气之前的数据")
    @PostMapping("/waterOrGasData")
    public Mono<Object> testWaterAndGas(@RequestBody GetIotDeviceEnergy getIotDeviceEnergy) {


        String devicePropety = iotService.getDevicePropety(getIotDeviceEnergy);

        List list = JSON.parseObject(devicePropety, List.class, Feature.OrderedField);
        ArrayList<String> strings1 = new ArrayList<>();
        for (Object o : list) {
            strings1.add(JSON.parseObject(String.valueOf(o), String.class, Feature.OrderedField));
        }

        if("0".equals(getIotDeviceEnergy.getTag())){
            //水
            //首先获取同步开始时间之前的最后一条的number
            return waterConsumeService
                .createQuery()
                .lt(WaterConsumeEntity::getGatherTime, getIotDeviceEnergy.getStartTime())
                .where(WaterConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                .orderBy(SortOrder.desc(WaterConsumeEntity::getGatherTime))
                .fetchOne()
                .flatMap(va -> {
                    //更新设备表的number
                    return deviceService
                        .createUpdate()
                        .set(DeviceInfoEntity::getMeterNumber, va.getNumber())
                        .where(DeviceInfoEntity::getDeviceId, getIotDeviceEnergy.getDeviceId())
                        .where(DeviceInfoEntity::getStatus, "0")
                        .execute()
                        .thenMany(
                            Flux.fromIterable(strings1)
                                .concatMap(value -> {
                                    String property = JSONObject.parseObject(value).get("property").toString();
                                    String deviceId = JSONObject.parseObject(value).get("deviceId").toString();
                                    String timestamp = JSONObject.parseObject(value).get("timestamp").toString();
                                    String number = JSONObject.parseObject(value).get("value").toString();
                                    HashMap<String, String> hashMap = new HashMap<>();
                                    hashMap.put("deviceId", deviceId);
                                    hashMap.put("timestamp", timestamp);
                                    HashMap<String, String> content = new HashMap<>();
                                    content.put(property, number);
                                    hashMap.put("properties", JSONObject.toJSONString(content));
                                    return iotAccessService.getDeviceDataReport(JSONObject.toJSONString(hashMap));
                                })
                        )
                        .then(
                            //更新结束时间后面数据的difference
                            waterConsumeService
                                .createQuery()
                                .gt(WaterConsumeEntity::getGatherTime, getIotDeviceEnergy.getEndTime())
                                .where(WaterConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                                .orderBy(SortOrder.asc(WaterConsumeEntity::getGatherTime))
                                .fetchOne()
                                .flatMap(time -> {
                                    //查询设备表的表数
                                    return deviceService
                                        .createQuery()
                                        .where(DeviceInfoEntity::getDeviceId, getIotDeviceEnergy.getDeviceId())
                                        .where(DeviceInfoEntity::getStatus, "0")
                                        .fetch()
                                        .collectList()
                                        .flatMap(listS -> {
                                            AtomicReference<BigDecimal> shareSize = new AtomicReference<>(BigDecimal.ZERO);
                                            return Flux.fromIterable(listS)
                                                .flatMap(deviceEntity -> {
                                                    if (deviceEntity.getDeviceType().equals("0")) {
                                                        shareSize.set(BigDecimal.valueOf(listS.stream()
                                                            .filter(e -> e.getDeviceType().equals("0")).count()));
                                                        return waterConsumeService
                                                            .createUpdate()
                                                            .where(WaterConsumeEntity::getGatherTime, time.getGatherTime())
                                                            .where(WaterConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                                                            .not(WaterConsumeEntity::getDeviceId, "0")
                                                            .set(WaterConsumeEntity::getDifference, time.getNumber().subtract(deviceEntity.getMeterNumber()).divide(shareSize.get()).setScale(2, RoundingMode.HALF_UP))
                                                            .execute();
                                                    } else {
                                                        shareSize.set(BigDecimal.valueOf(listS.stream()
                                                            .filter(e -> e.getDeviceType().equals("1")).count()));
                                                        return waterConsumeService
                                                            .createUpdate()
                                                            .where(WaterConsumeEntity::getGatherTime, time.getGatherTime())
                                                            .where(WaterConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                                                            .where(WaterConsumeEntity::getDeviceId, "0")
                                                            .set(WaterConsumeEntity::getDifference, time.getNumber().subtract(deviceEntity.getMeterNumber()).divide(shareSize.get()).setScale(2, RoundingMode.HALF_UP))
                                                            .execute();
                                                    }
                                                }).then();
                                        });
                                })
                        ).then(
                            //更新设备表表数
                            waterConsumeService
                                .createQuery()
                                .gt(WaterConsumeEntity::getGatherTime, getIotDeviceEnergy.getEndTime())
                                .where(WaterConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                                .orderBy(SortOrder.desc(WaterConsumeEntity::getGatherTime))
                                .fetchOne()
                                .flatMap(last -> {
                                    return deviceService
                                        .createUpdate()
                                        .set(DeviceInfoEntity::getMeterNumber, last.getNumber())
                                        .where(DeviceInfoEntity::getDeviceId, getIotDeviceEnergy.getDeviceId())
                                        .where(DeviceInfoEntity::getStatus, "0")
                                        .execute();
                                })
                        );
                });
        }else {
            //气
            //首先获取同步开始时间之前的最后一条的number
            return gasConsumeService
                .createQuery()
                .lt(GasConsumeEntity::getGatherTime, getIotDeviceEnergy.getStartTime())
                .where(GasConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                .orderBy(SortOrder.desc(GasConsumeEntity::getGatherTime))
                .fetchOne()
                .flatMap(va -> {
                    //更新设备表的number
                    return deviceService
                        .createUpdate()
                        .set(DeviceInfoEntity::getMeterNumber, va.getNumber())
                        .where(DeviceInfoEntity::getDeviceId, getIotDeviceEnergy.getDeviceId())
                        .where(DeviceInfoEntity::getStatus, "0")
                        .execute()
                        .thenMany(
                            Flux.fromIterable(strings1)
                                .concatMap(value -> {
                                    String property = JSONObject.parseObject(value).get("property").toString();
                                    String deviceId = JSONObject.parseObject(value).get("deviceId").toString();
                                    String timestamp = JSONObject.parseObject(value).get("timestamp").toString();
                                    String number = JSONObject.parseObject(value).get("value").toString();
                                    HashMap<String, String> hashMap = new HashMap<>();
                                    hashMap.put("deviceId", deviceId);
                                    hashMap.put("timestamp", timestamp);
                                    HashMap<String, String> content = new HashMap<>();
                                    content.put(property, number);
                                    hashMap.put("properties", JSONObject.toJSONString(content));
                                    return iotAccessService.getDeviceDataReport(JSONObject.toJSONString(hashMap));
                                })
                        )
                        .then(
                            //更新结束时间后面数据的difference
                            gasConsumeService
                                .createQuery()
                                .gt(GasConsumeEntity::getGatherTime, getIotDeviceEnergy.getEndTime())
                                .where(GasConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                                .orderBy(SortOrder.asc(GasConsumeEntity::getGatherTime))
                                .fetchOne()
                                .flatMap(time -> {
                                    //查询设备表的表数
                                    return deviceService
                                        .createQuery()
                                        .where(DeviceInfoEntity::getDeviceId, getIotDeviceEnergy.getDeviceId())
                                        .where(DeviceInfoEntity::getStatus, "0")
                                        .fetch()
                                        .collectList()
                                        .flatMap(listS -> {
                                            AtomicReference<BigDecimal> shareSize = new AtomicReference<>(BigDecimal.ZERO);
                                            return Flux.fromIterable(listS)
                                                .flatMap(deviceEntity -> {
                                                    if (deviceEntity.getDeviceType().equals("0")) {
                                                        shareSize.set(BigDecimal.valueOf(listS.stream()
                                                            .filter(e -> e.getDeviceType().equals("0")).count()));
                                                        return gasConsumeService
                                                            .createUpdate()
                                                            .where(GasConsumeEntity::getGatherTime, time.getGatherTime())
                                                            .where(GasConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                                                            .not(GasConsumeEntity::getDeviceId, "0")
                                                            .set(GasConsumeEntity::getDifference, time.getNumber().subtract(deviceEntity.getMeterNumber()).divide(shareSize.get()).setScale(2, RoundingMode.HALF_UP))
                                                            .execute();
                                                    } else {
                                                        shareSize.set(BigDecimal.valueOf(listS.stream()
                                                            .filter(e -> e.getDeviceType().equals("1")).count()));
                                                        return gasConsumeService
                                                            .createUpdate()
                                                            .where(GasConsumeEntity::getGatherTime, time.getGatherTime())
                                                            .where(GasConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                                                            .where(GasConsumeEntity::getDeviceId, "0")
                                                            .set(GasConsumeEntity::getDifference, time.getNumber().subtract(deviceEntity.getMeterNumber()).divide(shareSize.get()).setScale(2, RoundingMode.HALF_UP))
                                                            .execute();
                                                    }
                                                }).then();
                                        });
                                })
                        ).then(
                            //更新设备表表数
                            gasConsumeService
                                .createQuery()
                                .gt(GasConsumeEntity::getGatherTime, getIotDeviceEnergy.getEndTime())
                                .where(GasConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                                .orderBy(SortOrder.desc(GasConsumeEntity::getGatherTime))
                                .fetchOne()
                                .flatMap(last -> {
                                    return deviceService
                                        .createUpdate()
                                        .set(DeviceInfoEntity::getMeterNumber, last.getNumber())
                                        .where(DeviceInfoEntity::getDeviceId, getIotDeviceEnergy.getDeviceId())
                                        .where(DeviceInfoEntity::getStatus, "0")
                                        .execute();
                                })
                        );

                });
        }

    }

    @Operation(summary = "获取单个电之前的数据")
    @PostMapping("/electricitySingleData")
    public Mono<Object> testElectricitySingle(@RequestBody GetIotDeviceEnergy getIotDeviceEnergy) {
        String devicePropety = iotService.getElectricityData(getIotDeviceEnergy);
        List list = JSON.parseObject(devicePropety, List.class, Feature.OrderedField);
        ArrayList<String> strings1 = new ArrayList<>();
        for (Object o : list) {
            strings1.add(JSON.parseObject(String.valueOf(o), String.class, Feature.OrderedField));
        }
        //首先获取同步开始时间之前的最后一条的number
        return electricityConsumeService
            .createQuery()
            .lt(ElectricityConsumeEntity::getGatherTime, getIotDeviceEnergy.getStartTime())
            .where(ElectricityConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
            .orderBy(SortOrder.desc(ElectricityConsumeEntity::getGatherTime))
            .fetchOne()
            .flatMap(va -> {
                //更新设备表的number
                return deviceService
                    .createUpdate()
                    .set(DeviceInfoEntity::getMeterNumber, va.getNumber())
                    .where(DeviceInfoEntity::getDeviceId, getIotDeviceEnergy.getDeviceId())
                    .where(DeviceInfoEntity::getStatus, "0")
                    .execute()
                    .thenMany(
                        //同步数据
                        Flux.fromIterable(strings1)
                            .concatMap(value -> {
                                String deviceId = JSONObject.parseObject(value).get("deviceId").toString();
                                String timestamp = JSONObject.parseObject(value).get("timestamp").toString();
                                ObjectMapper objectMapper = new ObjectMapper();
                                Map map = null;
                                try {
                                    map = objectMapper.readValue(value, Map.class);
                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                                HashMap<String, String> hashMap = new HashMap<>();
                                hashMap.put("deviceId", deviceId);
                                hashMap.put("timestamp", timestamp);
                                hashMap.put("properties", JSONObject.toJSONString(map));
                                return iotAccessService.getDeviceDataReport(JSONObject.toJSONString(hashMap));
                            })
                    ).then(
                        //更新结束时间后面数据的difference
                        electricityConsumeService
                            .createQuery()
                            .gt(ElectricityConsumeEntity::getGatherTime, getIotDeviceEnergy.getEndTime())
                            .where(ElectricityConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                            .orderBy(SortOrder.asc(ElectricityConsumeEntity::getGatherTime))
                            .fetchOne()
                            .flatMap(time -> {
                                //查询设备表的表数
                                return deviceService
                                    .createQuery()
                                    .where(DeviceInfoEntity::getDeviceId, getIotDeviceEnergy.getDeviceId())
                                    .where(DeviceInfoEntity::getStatus, "0")
                                    .fetch()
                                    .collectList()
                                    .flatMap(listS -> {
                                        AtomicReference<BigDecimal> shareSize = new AtomicReference<>(BigDecimal.ZERO);
                                        return Flux.fromIterable(listS)
                                            .flatMap(deviceEntity -> {
                                                if (deviceEntity.getDeviceType().equals("0")) {
                                                    shareSize.set(BigDecimal.valueOf(listS.stream()
                                                        .filter(e -> e.getDeviceType().equals("0")).count()));
                                                    return electricityConsumeService
                                                        .createUpdate()
                                                        .where(ElectricityConsumeEntity::getGatherTime, time.getGatherTime())
                                                        .where(ElectricityConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                                                        .not(ElectricityConsumeEntity::getDeviceId, "0")
                                                        .set(ElectricityConsumeEntity::getDifference, time.getNumber().subtract(deviceEntity.getMeterNumber()).divide(shareSize.get()).setScale(2, RoundingMode.HALF_UP))
                                                        .execute();
                                                } else {
                                                    shareSize.set(BigDecimal.valueOf(listS.stream()
                                                        .filter(e -> e.getDeviceType().equals("1")).count()));
                                                    return electricityConsumeService
                                                        .createUpdate()
                                                        .where(ElectricityConsumeEntity::getGatherTime, time.getGatherTime())
                                                        .where(ElectricityConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                                                        .where(ElectricityConsumeEntity::getDeviceId, "0")
                                                        .set(ElectricityConsumeEntity::getDifference, time.getNumber().subtract(deviceEntity.getMeterNumber()).divide(shareSize.get()).setScale(2, RoundingMode.HALF_UP))
                                                        .execute();
                                                }
                                            }).then();
                                    });
                            })
                    ).then(
                        //更新设备表表数
                        electricityConsumeService
                            .createQuery()
                            .gt(ElectricityConsumeEntity::getGatherTime, getIotDeviceEnergy.getEndTime())
                            .where(ElectricityConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                            .orderBy(SortOrder.desc(ElectricityConsumeEntity::getGatherTime))
                            .fetchOne()
                            .flatMap(last -> {
                                return deviceService
                                    .createUpdate()
                                    .set(DeviceInfoEntity::getMeterNumber, last.getNumber())
                                    .where(DeviceInfoEntity::getDeviceId, getIotDeviceEnergy.getDeviceId())
                                    .where(DeviceInfoEntity::getStatus, "0")
                                    .execute();
                            })
                    );
            });
    }

    ;

    public Mono<Object> testElectricity(GetIotDeviceEnergy getIotDeviceEnergy) {
        String devicePropety = iotService.getElectricityData(getIotDeviceEnergy);
        List list = JSON.parseObject(devicePropety, List.class, Feature.OrderedField);
        ArrayList<String> strings1 = new ArrayList<>();
        for (Object o : list) {
            strings1.add(JSON.parseObject(String.valueOf(o), String.class, Feature.OrderedField));
        }
        //首先获取同步开始时间之前的最后一条的number
        return electricityConsumeService
            .createQuery()
            .lt(ElectricityConsumeEntity::getGatherTime, getIotDeviceEnergy.getStartTime())
            .where(ElectricityConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
            .orderBy(SortOrder.desc(ElectricityConsumeEntity::getGatherTime))
            .fetchOne()
            .flatMap(va -> {
                //更新设备表的number
                return deviceService
                    .createUpdate()
                    .set(DeviceInfoEntity::getMeterNumber, va.getNumber())
                    .where(DeviceInfoEntity::getDeviceId, getIotDeviceEnergy.getDeviceId())
                    .where(DeviceInfoEntity::getStatus, "0")
                    .execute()
                    .thenMany(
                        //同步数据
                        Flux.fromIterable(strings1)
                            .concatMap(value -> {
                                String deviceId = JSONObject.parseObject(value).get("deviceId").toString();
                                String timestamp = JSONObject.parseObject(value).get("timestamp").toString();
                                ObjectMapper objectMapper = new ObjectMapper();
                                Map map = null;
                                try {
                                    map = objectMapper.readValue(value, Map.class);
                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                                HashMap<String, String> hashMap = new HashMap<>();
                                hashMap.put("deviceId", deviceId);
                                hashMap.put("timestamp", timestamp);
                                hashMap.put("properties", JSONObject.toJSONString(map));
                                return iotAccessService.getDeviceDataReport(JSONObject.toJSONString(hashMap));
                            })
                    ).then(
                        //更新结束时间后面数据的difference
                        electricityConsumeService
                            .createQuery()
                            .gt(ElectricityConsumeEntity::getGatherTime, getIotDeviceEnergy.getEndTime())
                            .where(ElectricityConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                            .orderBy(SortOrder.asc(ElectricityConsumeEntity::getGatherTime))
                            .fetchOne()
                            .flatMap(time -> {
                                //查询设备表的表数
                                return deviceService
                                    .createQuery()
                                    .where(DeviceInfoEntity::getDeviceId, getIotDeviceEnergy.getDeviceId())
                                    .where(DeviceInfoEntity::getStatus, "0")
                                    .fetch()
                                    .collectList()
                                    .flatMap(listS -> {
                                        AtomicReference<BigDecimal> shareSize = new AtomicReference<>(BigDecimal.ZERO);
                                        return Flux.fromIterable(listS)
                                            .flatMap(deviceEntity -> {
                                                if (deviceEntity.getDeviceType().equals("0")) {
                                                    shareSize.set(BigDecimal.valueOf(listS.stream()
                                                        .filter(e -> e.getDeviceType().equals("0")).count()));
                                                    return electricityConsumeService
                                                        .createUpdate()
                                                        .where(ElectricityConsumeEntity::getGatherTime, time.getGatherTime())
                                                        .where(ElectricityConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                                                        .not(ElectricityConsumeEntity::getDeviceId, "0")
                                                        .set(ElectricityConsumeEntity::getDifference, time.getNumber().subtract(deviceEntity.getMeterNumber()).divide(shareSize.get()).setScale(2, RoundingMode.HALF_UP))
                                                        .execute();
                                                } else {
                                                    shareSize.set(BigDecimal.valueOf(listS.stream()
                                                        .filter(e -> e.getDeviceType().equals("1")).count()));
                                                    return electricityConsumeService
                                                        .createUpdate()
                                                        .where(ElectricityConsumeEntity::getGatherTime, time.getGatherTime())
                                                        .where(ElectricityConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                                                        .where(ElectricityConsumeEntity::getDeviceId, "0")
                                                        .set(ElectricityConsumeEntity::getDifference, time.getNumber().subtract(deviceEntity.getMeterNumber()).divide(shareSize.get()).setScale(2, RoundingMode.HALF_UP))
                                                        .execute();
                                                }
                                            }).then();
                                    });
                            })
                    ).then(
                        //更新设备表表数
                        electricityConsumeService
                            .createQuery()
                            .gt(ElectricityConsumeEntity::getGatherTime, getIotDeviceEnergy.getEndTime())
                            .where(ElectricityConsumeEntity::getReportDeviceId, getIotDeviceEnergy.getDeviceId())
                            .orderBy(SortOrder.desc(ElectricityConsumeEntity::getGatherTime))
                            .fetchOne()
                            .flatMap(last -> {
                                return deviceService
                                    .createUpdate()
                                    .set(DeviceInfoEntity::getMeterNumber, last.getNumber())
                                    .where(DeviceInfoEntity::getDeviceId, getIotDeviceEnergy.getDeviceId())
                                    .where(DeviceInfoEntity::getStatus, "0")
                                    .execute();
                            })
                    );
            });
    }

    ;

    @Operation(summary = "批量同步电之前的数据")
    @PostMapping("/electricityData")
    public Flux<Object> testElectricityAll(@RequestBody GetIotDeviceEnergy getIotDeviceEnergy) {
        ArrayList<String> totalDevice = new ArrayList<>();
        totalDevice.add("10kV-2HP3-PMC");
        totalDevice.add("10kV-2HP4-PMC");
        totalDevice.add("10kV-2HP5-PMC");
        totalDevice.add("10kV-1HP3-PMC");
        totalDevice.add("10kV-1HP4-PMC");
        totalDevice.add("10kV-1HP5-PMC");
        totalDevice.add("10kV-1HP6-PMC");
        totalDevice.add("10kV-1HP7-PMC");


        if ("0".equals(getIotDeviceEnergy.getType())) {
            //全部表
            return deviceService
                .createQuery()
                .in(DeviceInfoEntity::getEnergyType, EnergyType.electricity)
                .where(DeviceInfoEntity::getStatus, "0")
                .not(DeviceInfoEntity::getParentId,"0")
                .fetch()
                .filter(v->!v.getDeviceId().equals("GM-1") && !v.getDeviceId().equals("WM-2") && !v.getDeviceId().equals("WM-1"))
                .map(DeviceInfoEntity::getDeviceId)
                .distinct()
                .concatMap(deviceId -> {
                    getIotDeviceEnergy.setDeviceId(deviceId);
                    return this.testElectricity(getIotDeviceEnergy);
                });
        } else if ("1".equals(getIotDeviceEnergy.getType())) {
            //设备表
            return deviceService
                .createQuery()
                .in(DeviceInfoEntity::getEnergyType, EnergyType.electricity)
                .where(DeviceInfoEntity::getStatus, "0")
                .not(DeviceInfoEntity::getParentId,"0")
                .fetch()
                .map(DeviceInfoEntity::getDeviceId)
                .filter(i -> !totalDevice.contains(i) && !i.equals("GM-1") && !i.equals("WM-2") && !i.equals("WM-1"))
                .distinct()
                .concatMap(deviceId -> {
                    getIotDeviceEnergy.setDeviceId(deviceId);
                    return this.testElectricity(getIotDeviceEnergy);
                });
        } else {
            //总表
            return Flux.fromIterable(totalDevice)
                .concatMap(deviceId -> {
                    getIotDeviceEnergy.setDeviceId(deviceId);
                    return this.testElectricity(getIotDeviceEnergy);
                });
        }
    }
}
