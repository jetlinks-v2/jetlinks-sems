package org.jetlinks.project.busi.service;

import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.project.busi.entity.*;
import org.jetlinks.project.busi.entity.res.UnitDeviceRes;
import org.jetlinks.project.busi.service.event.EnergyGatherEventHandler;
import org.jetlinks.project.busi.utils.SnowflakeIdWorker;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;


@Service
@RequiredArgsConstructor
@Slf4j
public class IotAccessService {

    private final EnergyGatherService energyGatherService;
    private final DeviceService deviceService;
    private final CombinationUnitService unitService;
    private final CombinationUnitDeviceService unitDeviceService;
    private final QueryHelper queryHelper;
    private final EnergyGatherEventHandler energyGatherEventHandler;
    private final DeviceStateService deviceStateService;
    private final ReactiveRedisTemplate<Object,Object> redis;

    public Mono<Void> getDeviceDataReport(String deviceDate) {
        JSONObject device = JSONObject.parseObject(deviceDate);
        Object properties = device.get("properties");
        String deviceId = device.getString("deviceId");
        Long timestamp = device.getLong("timestamp");

        EnergyGatherEntity entity = new EnergyGatherEntity();
        entity.setDeviceId(deviceId);
        entity.setGatherTime(timestamp);
        entity.setContent(properties.toString());

        JSONObject content = JSONObject.parseObject(properties.toString());
        if(!content.containsKey("Flow") && !content.containsKey("positiveActE") && !content.containsKey("number")){
            return redis.hasKey("device:"+deviceId)
                        .flatMap(hasRedis ->{
                            if(hasRedis){
                                if (content.containsKey("computeStatus")) {
                                    String computeStatus = content.getString("computeStatus");
                                    return redis.opsForValue().get("device:"+deviceId)
                                                .mapNotNull(value->(DeviceInfoEntity) value)
                                                .flatMap(deviceInfoEntity -> {
                                                    if(!computeStatus.equals(deviceInfoEntity.getComputeStatus())){
                                                        return deviceService.createUpdate()
                                                            .where(DeviceInfoEntity::getDeviceId,entity.getDeviceId())
                                                            .set(DeviceInfoEntity::getComputeStatus,content.getString("computeStatus"))
                                                            .execute()
                                                            .flatMap(e-> deviceStateService
                                                                .createQuery()
                                                                .where(DeviceStateEntity::getDeviceId,deviceId)
                                                                .and(DeviceStateEntity::getComputeStatus,"3")
                                                                .fetch()
                                                                .collectList()
                                                                .flatMap(list ->{
                                                                    if(!list.isEmpty()){
                                                                        if(content.getString("computeStatus").equals("4")){
                                                                            deviceInfoEntity.setComputeStatus("4");
                                                                            return deviceStateService
                                                                                .createUpdate()
                                                                                .set(DeviceStateEntity::getEndTime,timestamp)
                                                                                .set(DeviceStateEntity::getComputeStatus,content.getString("computeStatus"))
                                                                                .where(DeviceStateEntity::getDeviceId,deviceId)
                                                                                .isNull(DeviceStateEntity::getEndTime)
                                                                                .execute()
                                                                                .then(redis.opsForValue().set("device:"+deviceId, deviceInfoEntity));
                                                                        }
                                                                    }else {
                                                                        if(content.getString("computeStatus").equals("3")) {
                                                                            DeviceStateEntity deviceStateEntity = new DeviceStateEntity();
                                                                            deviceStateEntity.setDeviceId(deviceId);
                                                                            deviceStateEntity.setComputeStatus(content.getString("computeStatus"));
                                                                            deviceStateEntity.setStartTime(timestamp);
                                                                            deviceInfoEntity.setComputeStatus("3");
                                                                            return deviceStateService.save(deviceStateEntity)
                                                                                .then(redis.opsForValue().set("device:"+deviceId, deviceInfoEntity));
                                                                        }
                                                                    }
                                                                    return Mono.empty();
                                                                }).then()
                                                            );
                                                    }
                                                    return Mono.empty();
                                                });
                                }
                                return Mono.empty();
                            }
                            return Mono.empty();
                        });
        }

        return redis.opsForValue()
            .get("device:"+deviceId)
            .switchIfEmpty(Mono.empty())
            .flatMap(e -> {
                String id = String.valueOf(new SnowflakeIdWorker().nextId());
                entity.setId(id);
                DeviceInfoEntity deviceInfoEntity =(DeviceInfoEntity) e;
                entity.setEnergyType(deviceInfoEntity.getEnergyType()[0]);
                return energyGatherService.save(entity);
            })
            .flatMap(gather -> energyGatherEventHandler.sendSaveNotify(entity));

    }

    /**
     * Iot设备上下线状态更新
     * @return
     */
    public Mono<Void> getDeviceState(@RequestBody String deviceState) {
        JSONObject device = JSONObject.parseObject(deviceState);
        String messageType = device.getString("messageType");
        String deviceId = device.getString("deviceId");
        String deviceStatus;
        if(messageType.equals("ONLINE")){
            deviceStatus = "1";
        } else {
            deviceStatus = "0";
        }

        return deviceService.createQuery()
                            .where(DeviceInfoEntity::getDeviceId, deviceId)
                            .and(DeviceInfoEntity::getStatus,"0")
                            .fetchOne()
                            .flatMap(e-> deviceService
                                .createUpdate()
                                .set(DeviceInfoEntity::getDeviceStatus, deviceStatus)
                                .where(DeviceInfoEntity::getDeviceId, deviceId)
                                .and(DeviceInfoEntity::getStatus,"0")
                                .execute()
                                .flatMapMany(v -> {
                                    return queryHelper
                                        .select("select count(*)  `count`  from sems_device_info\n" +
                                                    "where status='0' and device_status='0' and device_id in (select device_id from sems_combination_unit_device\n" +
                                                    "where unit_id in(select unit_id from sems_combination_unit_device\n" +
                                                    "where device_id = '"+deviceId+"'))" +
                                                    "and status='0' and device_status ='0'", UnitDeviceRes::new)
                                        .fetch()
                                        .flatMap(data -> {
                                            if(String.valueOf(data.getCount()).equals("0")){
                                                //如果设备状态全部为在线，修改组合设备的状态为在线
                                                return unitDeviceService
                                                    .createQuery()
                                                    .where(CombinationUnitDeviceEntity::getDeviceId,deviceId)
                                                    .fetch()
                                                    .flatMap(cud -> unitService
                                                        .createUpdate()
                                                        .set(CombinationUnitEntity::getDeviceStatus,"1")
                                                        .where(CombinationUnitDeviceEntity::getId,cud.getUnitId())
                                                        .execute());
                                            }
                                            //有一个是离线，组合设备即为离线
                                            return unitDeviceService
                                                .createQuery()
                                                .where(CombinationUnitDeviceEntity::getDeviceId,deviceId)
                                                .fetch()
                                                .flatMap(cud -> unitService
                                                    .createUpdate()
                                                    .set(CombinationUnitEntity::getDeviceStatus,"0")
                                                    .where(CombinationUnitDeviceEntity::getId,cud.getUnitId())
                                                    .execute());
                                        });
                                }).then());
    }


}
