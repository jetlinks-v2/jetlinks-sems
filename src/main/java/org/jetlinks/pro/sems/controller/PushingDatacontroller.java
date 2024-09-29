package org.jetlinks.pro.sems.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.sems.abutment.res.ReturnDataRes;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.jetlinks.pro.sems.entity.ElectricityConsumeEntity;
import org.jetlinks.pro.sems.entity.GasConsumeEntity;
import org.jetlinks.pro.sems.entity.WaterConsumeEntity;
import org.jetlinks.pro.sems.service.ElectricityConsumeService;
import org.jetlinks.pro.sems.service.GasConsumeService;
import org.jetlinks.pro.sems.service.WaterConsumeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/sems")
@AllArgsConstructor
@Getter
@Tag(name = "推送数据") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Slf4j
@Authorize(ignore = true)
public class PushingDatacontroller {

    private final WaterConsumeService waterConsumeService;

    private final ElectricityConsumeService electricityConsumeService;

    private final GasConsumeService gasConsumeService;

    //水
    @PostMapping("/getWaterData")
    @Operation(summary = "获取水的能耗数据")
    public Mono<ReturnDataRes> pushWater(@RequestBody QueryParamEntity queryParam){
        return waterConsumeService
            .createQuery()
            .setParam(queryParam)
            .fetch()
            .doOnNext(v->v.setUnitPrice(v.getDifference().multiply(v.getUnitPrice())))
            .collectList()
            .flatMap(list->{
                ReturnDataRes returnDataRes = new ReturnDataRes();
                //用量
                BigDecimal number = list.stream().map(WaterConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add);
                //金额
                BigDecimal cost = list.stream().map(WaterConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                returnDataRes.setNumber(number);
                returnDataRes.setCost(cost);
                return Mono.just(returnDataRes);
            });
    }

    //电
    @PostMapping("/getElectricityData")
    @Operation(summary = "获取电的能耗数据")
    public Mono<ReturnDataRes> pushElectricity(@RequestBody QueryParamEntity queryParam){
        return electricityConsumeService
            .createQuery()
            .setParam(queryParam)
            .fetch()
            .doOnNext(v->v.setUnitPrice(v.getDifference().multiply(v.getUnitPrice())))
            .collectList()
            .flatMap(list->{
                ReturnDataRes returnDataRes = new ReturnDataRes();
                //用量
                BigDecimal number = list.stream().map(ElectricityConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add);
                //金额
                BigDecimal cost = list.stream().map(ElectricityConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                returnDataRes.setNumber(number);
                returnDataRes.setCost(cost);
                return Mono.just(returnDataRes);
            });
    }

    //水
    @PostMapping("/getGasData")
    @Operation(summary = "获取气的能耗数据")
    public Mono<ReturnDataRes> push(@RequestBody QueryParamEntity queryParam){
        return gasConsumeService
            .createQuery()
            .setParam(queryParam)
            .fetch()
            .doOnNext(v->v.setUnitPrice(v.getDifference().multiply(v.getUnitPrice())))
            .collectList()
            .flatMap(list->{
                ReturnDataRes returnDataRes = new ReturnDataRes();
                //用量
                BigDecimal number = list.stream().map(GasConsumeEntity::getDifference).reduce(BigDecimal.ZERO, BigDecimal::add);
                //金额
                BigDecimal cost = list.stream().map(GasConsumeEntity::getUnitPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                returnDataRes.setNumber(number);
                returnDataRes.setCost(cost);
                return Mono.just(returnDataRes);
            });
    }
}
