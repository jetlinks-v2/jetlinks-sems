package org.jetlinks.project.busi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.annotation.*;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.DeviceInfoEntity;
import org.jetlinks.project.busi.entity.TestConfigDeviceEntity;
import org.jetlinks.project.busi.entity.WellHouseEntity;
import org.jetlinks.project.busi.entity.WellHouseMeterEntity;
import org.jetlinks.project.busi.entity.req.WellHouseReq;
import org.jetlinks.project.busi.entity.res.EnergyMeterRes;
import org.jetlinks.project.busi.service.WellHouseMeterService;
import org.jetlinks.project.busi.service.WellHouseService;
import org.jetlinks.project.busi.utils.SnowflakeIdWorker;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/well/house")
@AllArgsConstructor
@Getter
@Tag(name = "井房 1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "well-house", name = "井房")
public class WellHouseController implements AssetsHolderCrudController<WellHouseEntity,String> {

    private final WellHouseService service;

    private final WellHouseMeterService meterService;

    private final QueryHelper queryHelper;

    @Operation(summary = "根据ID查询井房详情")
    @GetMapping("/info/{id}")
    @QueryAction
    public Mono<WellHouseReq> queryWellHouseInfo(@PathVariable String id) {
        WellHouseReq wellHouseReq = new WellHouseReq();
        return service
            .findById(id)
            .flatMap(entity -> {
                wellHouseReq.setId(id);
                wellHouseReq.setEnergyType(entity.getEnergyType());
                wellHouseReq.setName(entity.getName());
                wellHouseReq.setPosition(entity.getPosition());
                return meterService
                    .createQuery()
                    .where(WellHouseMeterEntity::getWellHouseId,id)
                    .fetch()
                    .collectList()
                    .flatMap(list ->{
                        wellHouseReq.setMeterList(list);
                        return Mono.just(wellHouseReq);
                    });
            });
    }

    @Operation(summary = "新增数据2.0")
    @PostMapping("_insert/well_house")
    @SaveAction
    @Transactional(rollbackFor=Exception.class)
    public Mono<Integer> insertNew(@RequestBody WellHouseReq wellHouseReq) {
         String id = String.valueOf(new SnowflakeIdWorker().nextId());
         WellHouseEntity wellHouseEntity = new WellHouseEntity();
         wellHouseEntity.setId(id);
         wellHouseEntity.setName(wellHouseReq.getName());
         wellHouseEntity.setPosition(wellHouseReq.getPosition());
         wellHouseEntity.setEnergyType(wellHouseReq.getEnergyType());
         wellHouseEntity.setEnableStatus("1");
         return service.createQuery()
                       .where(WellHouseEntity::getName,wellHouseReq.getName())
                       .and(WellHouseEntity::getStatus,"0")
                       .fetch()
                       .collectList()
                       .flatMap(list ->{
                           if(list.size() >0){
                               return Mono.error(new RuntimeException("井房已存在，请重新输入！"));
                           }
                           return service.insert(wellHouseEntity)
                                         .flatMap(e ->{
                                             if(e > 0){
                                                 return Flux
                                                     .fromIterable(wellHouseReq.getMeterList())
                                                     .doOnNext(meterEntity -> meterEntity.setWellHouseId(id))
                                                     .collectList()
                                                     .flatMap(meterList ->meterService.insertBatch(Flux.just(meterList)));
                                             }
                                             return Mono.empty();
                                         });
                       });
    }

    @Operation(summary = "更新井房能源表")
    @PostMapping("/update/well/house")
    @ResourceAction(id= Permission.ACTION_UPDATE, name = "更新")
    public Mono<SaveResult> saveBindDevice(@RequestBody WellHouseReq wellHouseReq) {
        WellHouseEntity wellHouseEntity = new WellHouseEntity();
        wellHouseEntity.setId(wellHouseReq.getId());
        wellHouseEntity.setName(wellHouseReq.getName());
        wellHouseEntity.setPosition(wellHouseReq.getPosition());
        wellHouseEntity.setEnergyType(wellHouseReq.getEnergyType());
        return service.save(wellHouseEntity)
                      .flatMap(e ->{
                          if(e.getUpdated() > 0){
                              return meterService.createDelete()
                                                 .where(WellHouseMeterEntity::getWellHouseId,wellHouseReq.getId())
                                                 .execute()
                                                 .flatMap(del->Flux.fromIterable(wellHouseReq.getMeterList())
                                                           .doOnNext(meterEntity -> meterEntity.setWellHouseId(wellHouseReq.getId()))
                                                           .collectList()
                                                           .flatMap(meterService::save));
                          }
                          return Mono.empty();
                      });
    }


    @Operation(summary = "能源采集表详情")
    @GetMapping("/energy/meter/info/{id}")
    @Authorize(ignore = true)
    public Flux<EnergyMeterRes> energyMeterInfo(@PathVariable String id) {
        return queryHelper
            .select("SELECT \n" +
                        "t1.*, \n" +
                        "t2.device_name as deviceName, \n" +
                        "t3.id as areaId,\n" +
                        "t3.area_name as areaName \n" +
                        "FROM sems_energy_meter t1 \n" +
                        "LEFT JOIN sems_device_info t2 on t1.device_id=t2.device_id \n" +
                        "LEFT JOIN area_info t3 on t3.id=t2.area_id ", EnergyMeterRes::new)
            .where(dsl -> dsl.and("t1.device_id",  id)
                             .orderByDesc("t1.create_time")
                             .doPaging(0,1))
            .fetch();
    }

    @Operation(summary = "查询井房")
    @PostMapping("/query/list")
    @Authorize(ignore = true)
    public Flux<WellHouseEntity> queryList(@RequestBody QueryParamEntity query) {
        return service.createQuery().setParam(query).fetch();
    }

    @Operation(summary = "删除井房")
    @PostMapping("delete")
    @DeleteAction
    public Mono<Integer> deleteWellHouse(@RequestBody WellHouseEntity entity) {
        return service
            .createUpdate()
            .set(WellHouseEntity::getStatus,"1")
            .where(WellHouseEntity::getId,entity.getId())
            .execute()
            .then(meterService
                      .createDelete()
                      .where(WellHouseMeterEntity::getWellHouseId,entity.getId())
                      .execute()
            );
    }

}
