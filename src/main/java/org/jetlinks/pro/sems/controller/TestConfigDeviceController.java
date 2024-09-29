package org.jetlinks.pro.sems.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.pro.sems.entity.req.TestBindDeviceReq;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.jetlinks.pro.sems.entity.TestConfigDeviceEntity;
import org.jetlinks.pro.sems.service.TestConfigDeviceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/test/config/device")
@AllArgsConstructor
@Getter
@Tag(name = "试验配置设备映射 1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "config-device", name = "试验配置设备映射")
public class TestConfigDeviceController implements AssetsHolderCrudController<TestConfigDeviceEntity,String> {

    private final TestConfigDeviceService service;

    private final QueryHelper queryHelper;

    @Operation(summary = "绑定试验配置设备")
    @PostMapping("/_bind/_device")
    @ResourceAction(id= Permission.ACTION_UPDATE, name = "更新")
    public Mono<Integer> saveBindDevice(@RequestBody TestBindDeviceReq req) {

        List<TestConfigDeviceEntity> deviceList = req.getDeviceList();
        Collection<TestConfigDeviceEntity> collection = new ArrayList<>();
        for (TestConfigDeviceEntity deviceEntity:deviceList) {
            TestConfigDeviceEntity entity = new TestConfigDeviceEntity();
            entity.setConfigId(req.getConfigId());
            entity.setDeviceId(deviceEntity.getDeviceId());
            entity.setDeviceName(deviceEntity.getDeviceName());
            collection.add(entity);
        }

        Flux<Collection<TestConfigDeviceEntity>> just = Flux.just(collection);

        if(deviceList.size() < 1){
            //设备ID数组长度为0时，为解绑，删除所有映射
            return service
                .createDelete()
                .where(TestConfigDeviceEntity::getConfigId,req.getConfigId())
                .execute();
        } else {
            //先删除再添加
            return service
                .createDelete()
                .where(TestConfigDeviceEntity::getConfigId,req.getConfigId())
                .execute()
                .then(service.insertBatch(just));
        }
    }

}
