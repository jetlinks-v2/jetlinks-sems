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
import org.jetlinks.pro.sems.entity.req.EnergyRatioReq;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.jetlinks.pro.sems.entity.EnergyRatioEntity;
import org.jetlinks.pro.sems.service.EnergyRatioService;
import org.jetlinks.pro.sems.service.OperateLogService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/energy/ratio")
@AllArgsConstructor
@Getter
@Tag(name = "能耗占比配置 1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "energy-ratio", name = "能耗占比配置")
public class EnergyRatioController implements AssetsHolderCrudController<EnergyRatioEntity,String> {
    private final EnergyRatioService service;

    private final QueryHelper queryHelper;

    private final OperateLogService operateLogService;

    @Operation(summary = "修改能耗占比配置")
    @PostMapping("_update/energy/ratio")
    @ResourceAction(id= Permission.ACTION_UPDATE, name = "更新")
    public Mono<Boolean> updateEnergyRatio(@RequestBody EnergyRatioReq req) {
        return service.updateById(req.getEnergyRatioEntity().getId(),req.getEnergyRatioEntity())
                      .then(operateLogService.save(req.getOperateLogList()))
                      .thenReturn(true);
    }
}