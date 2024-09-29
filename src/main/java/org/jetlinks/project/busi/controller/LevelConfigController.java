package org.jetlinks.project.busi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.LevelConfigEntity;
import org.jetlinks.project.busi.enums.EnergyQueryTypeEnum;
import org.jetlinks.project.busi.enums.EnergyType;
import org.jetlinks.project.busi.service.LevelConfigService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.function.Function;

/**
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.0
 */
@RestController
@RequestMapping("/sems/level/config")
@AllArgsConstructor
@Getter
@Tag(name = "能源等级配置表1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "level-config", name = "能源等级配置表")
public class LevelConfigController implements ReactiveServiceCrudController<LevelConfigEntity,String> {

    private final LevelConfigService service;


    @Operation(summary = "新增/修改")
    @PostMapping("/_add/_update")
    @QueryAction
    @Transactional
    public Flux<Object> addOrUpdate(@RequestBody Flux<LevelConfigEntity> payload) {
        //先删除
        return payload
            .collectList()
            .flatMap(li -> {
                if (!li.isEmpty()) {
                    EnergyQueryTypeEnum type = li.get(0).getType();
                    EnergyType energyType = li.get(0).getEnergyType();
                    return service.createDelete()
                        .where(LevelConfigEntity::getType, type)
                        .where(LevelConfigEntity::getEnergyType, energyType)
                        .execute();
                }
                return Mono.empty();
            }).thenMany(payload.collectList().flatMapMany(i -> {
                    for (LevelConfigEntity entity : i) {
                        if (entity.getStartNumber().compareTo(entity.getEndNumber()) == 1)
                            throw new BusinessException("错误：开始区间不能小于结束区间", 500);
                    }
                    i.sort(Comparator.comparing(LevelConfigEntity::getStartNumber));
                    if (i.size() <= 1) return Flux.fromIterable(i).as(getService()::save);
                    for (int j = 1; j < i.size(); j++) {
                        if (i.get(j).getStartNumber().compareTo(i.get(j - 1).getEndNumber()) == -1) {
                            throw new BusinessException("错误：存在区间重叠", 500);
                        }
                    }
                    return Flux.fromIterable(i).as(getService()::save);
                })
            );
    }
}
