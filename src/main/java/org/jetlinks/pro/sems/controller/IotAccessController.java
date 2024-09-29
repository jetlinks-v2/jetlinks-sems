package org.jetlinks.pro.sems.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.sems.entity.res.UnitDeviceRes;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.jetlinks.pro.sems.entity.CombinationUnitDeviceEntity;
import org.jetlinks.pro.sems.entity.CombinationUnitEntity;
import org.jetlinks.pro.sems.entity.DeviceInfoEntity;
import org.jetlinks.pro.sems.entity.EnergyGatherEntity;
import org.jetlinks.pro.sems.service.CombinationUnitDeviceService;
import org.jetlinks.pro.sems.service.CombinationUnitService;
import org.jetlinks.pro.sems.service.DeviceService;
import org.jetlinks.pro.sems.service.EnergyGatherService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/iot/access")
@AllArgsConstructor
@Getter
@Tag(name = "Iot平台接入 1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
public class IotAccessController {

    private final EnergyGatherService energyGatherService;

    private final DeviceService deviceService;

    private final CombinationUnitService unitService;

    private final CombinationUnitDeviceService unitDeviceService;

    private final QueryHelper queryHelper;

    /**
     * 从iot获取设备列表
     * @return
     */
    @Operation(summary = "Iot设备数据上报")
    @PostMapping("/device/data/report")
    @Authorize(ignore = true)
    public Mono<SaveResult> getDeviceDataReport(@RequestBody String deviceDate) {
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
            return Mono.empty();
        }

        return deviceService
            .createQuery()
            .where(DeviceInfoEntity::getDeviceId,entity.getDeviceId())
            .and(DeviceInfoEntity::getStatus,"0")
            .fetchOne()
            .flatMap(e -> {
                entity.setEnergyType(e.getEnergyType()[0]);
                return energyGatherService.save(entity);
            });

    }

    /**
     * Iot设备上下线状态更新
     * @return
     */
    @Operation(summary = "Iot设备上下线状态更新")
    @PostMapping("/device/state")
    @Authorize(ignore = true)
    public Flux<Object> getDeviceState(@RequestBody String deviceState) {
        JSONObject device = JSONObject.parseObject(deviceState);
        String messageType = device.getString("messageType");
        String deviceId = device.getString("deviceId");
        String deviceStatus = "0";
        if(messageType.equals("ONLINE")){
            deviceStatus = "1";
        }

        return deviceService.createUpdate()
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
                            return unitDeviceService.createQuery()
                                .where(CombinationUnitDeviceEntity::getDeviceId,deviceId)
                                .fetch()
                                .flatMap(cud -> unitService.createUpdate()
                                    .set(CombinationUnitEntity::getStatus,"1")
                                    .where(CombinationUnitDeviceEntity::getId,cud.getUnitId())
                                    .execute());
                        }
                        //有一个是离线，组合设备即为离线
                        return unitDeviceService.createQuery()
                            .where(CombinationUnitDeviceEntity::getDeviceId,deviceId)
                            .fetch()
                            .flatMap(cud -> unitService.createUpdate()
                                .set(CombinationUnitEntity::getStatus,"0")
                                .where(CombinationUnitDeviceEntity::getId,cud.getUnitId())
                                .execute());
                    });
            });
    }

}
