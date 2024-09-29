package org.jetlinks.project.busi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.AlarmRecordsEntity;
import org.jetlinks.project.busi.entity.DeviceInfoEntity;
import org.jetlinks.project.busi.entity.WellHouseMeterEntity;
import org.jetlinks.project.busi.entity.res.EnergyMeterRes;
import org.jetlinks.project.busi.service.WellHouseMeterService;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/well/house/meter")
@AllArgsConstructor
@Getter
@Tag(name = "井房能耗关系 1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "well-house-meter", name = "井房能耗表")
public class WellHouseMeterController implements AssetsHolderCrudController<WellHouseMeterEntity,String> {

    private final WellHouseMeterService service;

    private final QueryHelper queryHelper;


    @Operation(summary = "查询井房能耗表")
    @PostMapping("/query/list")
    @Authorize(ignore = true)
    public Flux<WellHouseMeterEntity> queryList(@RequestBody QueryParamEntity query) {
        return service.createQuery()
                      .setParam(query)
                      .fetch()
                      .flatMap(entity -> queryHelper
                              .select("SELECT \n" +
                                          "t1.*, \n" +
                                          "t2.device_name as deviceName, \n" +
                                          "t3.id as areaId,\n" +
                                          "t3.area_name as areaName \n" +
                                          "FROM sems_energy_meter t1 \n" +
                                          "LEFT JOIN sems_device_info t2 on t1.device_id=t2.device_id \n" +
                                          "LEFT JOIN area_info t3 on t3.id=t2.area_id ", EnergyMeterRes::new)
                              .where(dsl -> dsl.and("t1.device_id",  entity.getDeviceId())
                                               .orderByDesc("t1.create_time")
                                               .doPaging(0,1))
                              .fetch()
                              .collectList()
                              .flatMap(meterList ->{
                                  if(meterList.size() > 0){
                                      entity.setEnergyMeterStatus(true);
                                  } else {
                                      entity.setEnergyMeterStatus(false);
                                  }
                                  return Mono.just(entity);
                              })
                      ).sort(Comparator.comparing(WellHouseMeterEntity::getEnergyMeterStatus)
                                       .reversed());
    }

    @Operation(summary = "校验能耗表是否添加")
    @PostMapping("/check/only")
    @Authorize(ignore = true)
    public Flux<Object> CheckOnly(@RequestBody List<String> list) {
        return Flux.fromIterable(list)
                   .flatMap(entity ->{
                       return service
                           .createQuery()
                           .where(WellHouseMeterEntity::getDeviceId, entity)
                           .and(WellHouseMeterEntity::getStatus,"eq","0")
                           .fetch()
                           .collectList()
                           .flatMap(v -> {
                               if(v.size()==0){
                                   return Mono.just("0");
                               }
                               return Mono.just("1");
                           });
                   });
    }

}
