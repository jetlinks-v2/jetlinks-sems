package org.jetlinks.pro.sems.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.jetlinks.pro.sems.entity.CombinationUnitDeviceEntity;
import org.jetlinks.pro.sems.entity.CombinationUnitEntity;
import org.jetlinks.pro.sems.entity.DeviceInfoEntity;
import org.jetlinks.pro.sems.service.CombinationUnitDeviceService;
import org.jetlinks.pro.sems.service.CombinationUnitService;
import org.jetlinks.pro.sems.service.DeviceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/sems/unit")
@AllArgsConstructor
@Getter
@Tag(name = "组合设备1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "unit-device", name = "组合设备")
public class CombinationUnitController implements AssetsHolderCrudController<CombinationUnitEntity,String> {

    private CombinationUnitService service;

    private final QueryHelper queryHelper;

    private final CombinationUnitDeviceService combinationUnitDeviceService;

    private final DeviceService deviceService;

    @Operation(summary = "获取带区域的组合设备列表")
    @Authorize(ignore = true)
    @PostMapping("/_query/addAreaName")
    @QueryAction
    public Mono<PagerResult<CombinationUnitEntity>> queryAddAreaName(@RequestBody QueryParamEntity queryParamEntity){
        return queryHelper
            .select("select cu.id id,cu.unit_name unitName,cu.energy_type energyType,cu.`status` `status`,GROUP_CONCAT(DISTINCT ar.area_name) areaName, cu.create_time as createTime from sems_combination_unit cu\n" +
                "left join sems_combination_unit_device cud\n" +
                "on cu.id = cud.unit_id\n" +
                "left join sems_device_info de\n" +
                "on de.device_id = cud.device_id\n" +
                "left join area_info ar\n" +
                "on ar.id = de.area_id\n" +
                "where  de.status ='0'\n" +
                "group by cu.id",CombinationUnitEntity::new)
            .where(queryParamEntity)
            .fetchPaged();
    }

    @Operation(summary = "获取带设备的信息")
    @Authorize(ignore = true)
    @PostMapping("/_query/addDeviceName")
    @QueryAction
    public Flux<CombinationUnitEntity> queryAddDeviceName(@RequestBody QueryParamEntity queryParamEntity){
        return service
            .query(queryParamEntity)
            .flatMap(value->{
                //查询设备
                return combinationUnitDeviceService
                    .createQuery()
                    .where(CombinationUnitDeviceEntity::getUnitId,value.getId())
                    .fetch()
                    .flatMap(li->{
                        return deviceService
                            .createQuery()
                            .where(DeviceInfoEntity::getDeviceId,li.getDeviceId())
                            .fetchOne();
                    }).map(DeviceInfoEntity::getDeviceName)
                    .collectList()
                    .doOnNext(list->value.setDeviceNames(list))
                    .thenReturn(value);
            });
    }
}
