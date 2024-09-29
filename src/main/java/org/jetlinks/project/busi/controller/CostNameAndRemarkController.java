package org.jetlinks.project.busi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.DeleteAction;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.CostConfigEntity;
import org.jetlinks.project.busi.entity.CostNameAndRemarkEntity;
import org.jetlinks.project.busi.entity.ElectricityIntervalEntity;
import org.jetlinks.project.busi.service.CostConfService;
import org.jetlinks.project.busi.service.CostNameAndRemarkService;
import org.jetlinks.project.busi.service.ElectricityIntervalService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@RestController
@RequestMapping("/cost/name")
@AllArgsConstructor
@Getter
@Tag(name = "1.0 费用配置名称") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "cost-name", name = "费用配置名称")
public class CostNameAndRemarkController implements AssetsHolderCrudController<CostNameAndRemarkEntity,String> {
    private final CostNameAndRemarkService service;

    private final CostConfService costConfService;

    private final ElectricityIntervalService electricityIntervalService;


    /**
     * 删除费用配置，逻辑删除
     * @return
     */
    @Operation(summary = "删除费用配置，逻辑删除")
    @DeleteMapping("/del/config/{id}")
    @DeleteAction
    public Mono<Integer> delConfig(@PathVariable("id") String id){
        return service
            .createUpdate()
            .set(CostNameAndRemarkEntity::getState,"2")
            .where(CostNameAndRemarkEntity::getId,id)
            .execute()
            .then(costConfService.createUpdate()
                .set(CostConfigEntity::getState,"2")
                .where(CostConfigEntity::getCostConfigNameId,id)
                .execute()
            ).then(
                costConfService.createQuery()
                        .where(CostConfigEntity::getCostConfigNameId,id)
                            .where(CostConfigEntity::getEnergyType,"2")
                                .fetchOne()
                                    .flatMap(i->{
                                        return electricityIntervalService
                                            .createUpdate()
                                            .set(ElectricityIntervalEntity::getState,"2")
                                            .where(ElectricityIntervalEntity::getCostConfigId,i.getId())
                                            .execute();
                                    })
                );
    }

    @PostMapping("/list/extend")
    @Operation(summary = "费用配置列表扩展")
    @QueryAction
    public Mono<PagerResult<CostNameAndRemarkEntity>> listExtend(@RequestBody QueryParamEntity queryParam){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return service
            .queryPager(queryParam)
            .flatMap(list->{
                return Flux.fromIterable(list.getData())
                    .flatMap(cost->{
                        return costConfService
                            .createQuery()
                            .where(CostConfigEntity::getCostConfigNameId,cost.getId())
                            .where(CostConfigEntity::getEnergyType,"1")
                            .fetchOne()
                            .flatMap(co->{
                                Date start = new Date(co.getEffectiveTimeIntervalStartDate());
                                Date end = new Date(co.getEffectiveTimeIntervalEndDate());
                                cost.setWaterEffectTime(simpleDateFormat.format(start) +"~" +simpleDateFormat.format(end));
                                return Mono.just(cost);
                            }).then(
                                costConfService
                                    .createQuery()
                                    .where(CostConfigEntity::getCostConfigNameId,cost.getId())
                                    .where(CostConfigEntity::getEnergyType,"3")
                                    .fetchOne()
                                    .flatMap(co-> {
                                        Date start = new Date(co.getEffectiveTimeIntervalStartDate());
                                        Date end = new Date(co.getEffectiveTimeIntervalEndDate());
                                        cost.setGasEffectTime(simpleDateFormat.format(start) + "~" + simpleDateFormat.format(end));
                                        return Mono.just(cost);
                                    })
                                    ).then(
                                costConfService
                                    .createQuery()
                                    .where(CostConfigEntity::getCostConfigNameId,cost.getId())
                                    .where(CostConfigEntity::getEnergyType,"2")
                                    .fetchOne()
                                    .flatMap(co-> {

                                        return electricityIntervalService
                                            .createQuery()
                                                .where(ElectricityIntervalEntity::getCostConfigId,co.getId())
                                                    .fetch()
                                                        .flatMap(cos->{
                                                            String year=cos.getYearStart()+"~"+cos.getYearEnd();
                                                            return Mono.just(year);
                                                        }).doOnNext(value->cost.setElectricityEffectTime(value)).then();
                                    }).thenReturn(cost)
                            );
                    }).collectList()
                    .doOnNext(lis->list.setData(lis))
                    .thenReturn(list);
            });

    }
}
