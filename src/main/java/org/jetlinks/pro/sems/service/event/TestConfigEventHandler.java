package org.jetlinks.pro.sems.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.events.EntityBeforeCreateEvent;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.jetlinks.pro.sems.entity.EnergyRatioEntity;
import org.jetlinks.pro.sems.entity.TestConfigEntity;
import org.jetlinks.pro.sems.entity.res.EnergyRatioRes;
import org.jetlinks.pro.sems.service.EnergyRatioService;
import org.jetlinks.pro.sems.service.TestConfigService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class TestConfigEventHandler {

    private final TestConfigService service;
    private final EnergyRatioService energyRatioService;

    @EventListener
    public void handleCreatedEvent(EntityBeforeCreateEvent<TestConfigEntity> event) {

        event.async(this.sendBeforeCreatedNotify(event.getEntity()));

    }

    @EventListener
    public void handleCreatedEvent(EntityCreatedEvent<TestConfigEntity> event) {

        event.async(this.sendCreatedNotify(event.getEntity()));

    }


    public Mono<Void> sendBeforeCreatedNotify(List<TestConfigEntity> testConfigList) {
        return Flux.fromIterable(testConfigList)
                   .flatMap(testConfigEntity -> service
                       .createQuery()
                       .where(TestConfigEntity::getTestName,testConfigEntity.getTestName())
                       .and(TestConfigEntity::getStatus,"0")
                       .fetch()
                       .collectList()
                       .flatMap(list ->{
                           if(list.size() > 0){
                               return Mono.error(new RuntimeException("试验条目已存在，请重新输入！"));
                           }
                           return Mono.empty();
                       })
                   ).then();
    }

    public Mono<Void> sendCreatedNotify(List<TestConfigEntity> testConfigList) {
        return Flux.fromIterable(testConfigList)
                   .flatMap(testConfigEntity ->{
                       EnergyRatioEntity energyRatioEntity = defineEnergyRationEntity(testConfigEntity);
                       return energyRatioService.insert(energyRatioEntity);
                   })
                   .then();
    }

    private EnergyRatioEntity defineEnergyRationEntity(TestConfigEntity testConfigEntity) {
        EnergyRatioEntity energyRatioEntity = new EnergyRatioEntity();
        energyRatioEntity.setConfigId(testConfigEntity.getId());
        energyRatioEntity.setTestConfigName(testConfigEntity.getTestName());
        Map<String,String> placeMap = new HashMap<>();

        placeMap.put("1","CWT");
        placeMap.put("2","AAWT");
        placeMap.put("3","Soak3环境仓");
        placeMap.put("4","Soak1浸车室");
        placeMap.put("5","Soak2浸车室");
        placeMap.put("6","Soak4浸车室");

        List<EnergyRatioRes> ratioAirCondition = new ArrayList<>();
        List<EnergyRatioRes> ordinaryFreezing = new ArrayList<>();
        List<EnergyRatioRes> combinedCoolingTower = new ArrayList<>();
        List<EnergyRatioRes> combinedCoolingTowerAuto = new ArrayList<>();
        List<EnergyRatioRes> boiler = new ArrayList<>();
        List<EnergyRatioRes> airCompressor = new ArrayList<>();
        List<EnergyRatioRes> airCompressorPneumatic = new ArrayList<>();

        for (Map.Entry<String,String> map:placeMap.entrySet()){
            EnergyRatioRes res = new EnergyRatioRes();
            res.setPlaceId(map.getKey());
            res.setPlaceName(map.getValue());
            res.setEnergyRadio(BigDecimal.ZERO);

            ratioAirCondition.add(res);
            ordinaryFreezing.add(res);
            combinedCoolingTower.add(res);
            combinedCoolingTowerAuto.add(res);
            boiler.add(res);
            airCompressor.add(res);
            airCompressorPneumatic.add(res);
        }

        energyRatioEntity.setRatioAirCondition(ratioAirCondition);
        energyRatioEntity.setOrdinaryFreezing(ordinaryFreezing);
        energyRatioEntity.setCombinedCoolingTower(combinedCoolingTower);
        energyRatioEntity.setCombinedCoolingTowerAuto(combinedCoolingTowerAuto);
        energyRatioEntity.setBoiler(boiler);
        energyRatioEntity.setAirCompressor(airCompressor);
        energyRatioEntity.setAirCompressorPneumatic(airCompressorPneumatic);

        return energyRatioEntity;
    }


}
