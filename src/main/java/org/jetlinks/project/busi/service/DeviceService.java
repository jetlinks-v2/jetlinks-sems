package org.jetlinks.project.busi.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.project.busi.entity.DeviceInfoEntity;
import org.jetlinks.project.busi.enums.EnergyType;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceService extends GenericReactiveCrudService<DeviceInfoEntity,String> {


    //返回该区域对应总表id或者设备id
    public Mono<List<String>> getDeviceIdByAreaId(String areaId, Integer energyType){

        DeviceInfoEntity deviceInfo = new DeviceInfoEntity();
        ArrayList<String> result = new ArrayList<>();
        return this.createQuery()
            .where(DeviceInfoEntity::getParentFlag,"1")
            .where(DeviceInfoEntity::getEnergyType,energyType)
            .where(DeviceInfoEntity::getParentId,areaId)
            .not(DeviceInfoEntity::getStatus,"1")
            .fetch()
            .collectList()
            .flatMap(value->{
                if(!value.isEmpty()){
                    result.addAll(value.stream().map(DeviceInfoEntity::getDeviceId).collect(Collectors.toList()));
                    return Mono.just(result);
                }else {
                    return this.createQuery()
                        .where(DeviceInfoEntity::getParentFlag,"0")
                        .where(DeviceInfoEntity::getEnergyType,energyType)
                        .where(DeviceInfoEntity::getAreaId,areaId)
                        .where(DeviceInfoEntity::getParentId,"0")
                        .fetch()
                        .map(DeviceInfoEntity::getDeviceId)
                        .distinct()
                        .collectList();
                }
            });

    }
}
