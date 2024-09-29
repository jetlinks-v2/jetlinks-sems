package org.jetlinks.pro.sems.strategy.cost.handler;

import org.jetlinks.pro.sems.entity.AreaInfoEntity;
import org.jetlinks.pro.sems.entity.DeviceInfoEntity;
import org.jetlinks.pro.sems.entity.TestConfigDeviceEntity;
import org.jetlinks.pro.sems.entity.TestConfigEntity;
import org.jetlinks.pro.sems.entity.res.CostRes;
import org.jetlinks.pro.sems.entity.res.PeakUniversalData;
import org.jetlinks.pro.sems.service.AreaInfoService;
import org.jetlinks.pro.sems.service.DeviceService;
import org.jetlinks.pro.sems.service.TestConfigDeviceService;
import org.jetlinks.pro.sems.service.TestConfigService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @ClassName CostProportionHandler
 * @Author hky
 * @Time 2023/7/19 23:21
 * @Description
 **/
public class CostProportionHandler {



    public static Flux<CostRes> buildData(Map<String, BigDecimal> map,
                                          DeviceService deviceService,
                                          AreaInfoService areaInfoService) {
        Set<String> deviceIds = map.keySet();
        return deviceService
            .createQuery()
            .in(DeviceInfoEntity::getDeviceId, deviceIds)
            .fetch()
            .collectMultimap(DeviceInfoEntity::getAreaId, DeviceInfoEntity::getDeviceId)
            .flatMapMany(deviceMap -> {
                Set<String> areaIds = deviceMap.keySet();
                return areaInfoService
                    .createQuery()
                    .in(AreaInfoEntity::getId, areaIds)
                    .fetch()
                    .collectMap(AreaInfoEntity::getId, AreaInfoEntity::getAreaName)
                    .flatMapMany(areaMap -> Flux
                        .fromIterable(deviceMap.entrySet())
                        .flatMap(deviceEntry -> Flux
                            .fromIterable(map.entrySet())
                            .filter(dataEntry -> deviceEntry
                                .getValue()
                                .contains(dataEntry.getKey()))
                            .reduce((dataEntry, dataEntry2) -> {
                                dataEntry.setValue(dataEntry
                                                       .getValue()
                                                       .add(dataEntry2.getValue()));
                                return dataEntry;
                            })
                            .map(reduceEntry -> CostRes
                                .builder()
                                .region(areaMap.get(deviceEntry.getKey()))
                                .cost(reduceEntry.getValue())
                                .build())

                        )
                    );
            });
    }

    public static Flux<CostRes> buildTestData(Map<String, BigDecimal> map,
                                              TestConfigDeviceService testConfigDeviceService,
                                              TestConfigService testConfigService) {
        Set<String> deviceIds = map.keySet();
        //获取prentIds

                return testConfigDeviceService
                    .createQuery()
                    .in(DeviceInfoEntity::getDeviceId, deviceIds)
                    .fetch()
                    .collectMap(TestConfigDeviceEntity::getConfigId, TestConfigDeviceEntity::getDeviceId)
                    .flatMapMany(configMap -> {
                        Set<String> configIds = configMap.keySet();
                        return testConfigService
                            .createQuery()
                            .in(TestConfigEntity::getId, configIds)
                            .fetch()
                            .collectMap(TestConfigEntity::getId, TestConfigEntity::getTestName)
                            .flatMapMany(testMap -> Flux
                                .fromIterable(configMap.entrySet())
                                .flatMap(deviceEntry -> Flux
                                    .fromIterable(map.entrySet())
                                    .filter(dataEntry -> deviceEntry
                                        .getValue()
                                        .contains(dataEntry.getKey()))
                                    .reduce((dataEntry, dataEntry2) -> {
                                        dataEntry.setValue(dataEntry
                                            .getValue()
                                            .add(dataEntry2.getValue()));
                                        return dataEntry;
                                    })
                                    .map(reduceEntry -> CostRes
                                        .builder()
                                        .testName(testMap.get(deviceEntry.getKey()))
                                        .cost(reduceEntry.getValue())
                                        .build())
                                )
                            );
                    });
    }

    public static Mono<Map<String,BigDecimal>> reduceTotalValue(Map<String,Collection<PeakUniversalData>> map) {
        Map<String, BigDecimal> dataHashMap = new HashMap<>();
        for (Map.Entry<String, Collection<PeakUniversalData>> stringCollectionEntry : map.entrySet()) {
            String key = stringCollectionEntry.getKey();

            Collection<PeakUniversalData> value = stringCollectionEntry.getValue();
            BigDecimal total=BigDecimal.ZERO;
            for (PeakUniversalData peakUniversalData : value) {
                total=total.add(peakUniversalData.getUnitPrice().multiply(peakUniversalData.getTotalValue()).setScale(2, RoundingMode.HALF_UP));
            }
            dataHashMap.put(key,total);
        }
        return Mono.just(dataHashMap);


    }

    public static Mono<PeakUniversalData> reduceElectricValue(GroupedFlux<String, GroupedFlux<String,PeakUniversalData>> group) {
        return null;
    }


    public static Flux<CostRes> buildElectricData(Map<String, BigDecimal> map,
                                          DeviceService deviceService,
                                          AreaInfoService areaInfoService) {
        Set<String> deviceIds = map.keySet();
        return deviceService
            .createQuery()
            .in(DeviceInfoEntity::getDeviceId, deviceIds)
            .fetch()
            .collectMultimap(DeviceInfoEntity::getAreaId, DeviceInfoEntity::getDeviceId)
            .flatMapMany(deviceMap -> {
                Set<String> areaIds = deviceMap.keySet();
                return areaInfoService
                    .createQuery()
                    .in(AreaInfoEntity::getId, areaIds)
                    .fetch()
                    .collectMap(AreaInfoEntity::getId, AreaInfoEntity::getAreaName)
                    .flatMapMany(areaMap -> Flux
                        .fromIterable(deviceMap.entrySet())
                        .flatMap(deviceEntry -> Flux
                            .fromIterable(map.entrySet())
                            .filter(dataEntry -> deviceEntry
                                .getValue()
                                .contains(dataEntry.getKey()))
                            .reduce((dataEntry, dataEntry2) -> {
                                dataEntry.setValue(dataEntry
                                    .getValue()
                                    .add(dataEntry2.getValue()));
                                return dataEntry;
                            })
                            .map(reduceEntry -> CostRes
                                .builder()
                                .region(areaMap.get(deviceEntry.getKey()))
                                .cost(reduceEntry.getValue())
                                .build())

                        )
                    );
            });
    }
}
