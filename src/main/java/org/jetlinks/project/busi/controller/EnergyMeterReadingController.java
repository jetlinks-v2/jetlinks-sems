package org.jetlinks.project.busi.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.pro.io.excel.ExcelUtils;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.*;
import org.jetlinks.project.busi.entity.res.EnergyMeterReturnRes;
import org.jetlinks.project.busi.enums.EnergyType;
import org.jetlinks.project.busi.service.*;
import org.jetlinks.project.busi.utils.DateUtil;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/sems/energy/meter")
@AllArgsConstructor
@Getter
@Tag(name = "能耗抄表1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Slf4j
@Resource(id = "energy-meter", name = "能耗抄表")
public class EnergyMeterReadingController implements AssetsHolderCrudController<EnergyMeterReadingEntity,String> {

    private final EnergyMeterReadingService service;

    private final ElectricityConsumeService electricityConsumeService;

    private final GasConsumeService gasConsumeService;

    private final WaterConsumeService waterConsumeService;

    private final QueryHelper queryHelper;

    private final DeviceService deviceService;


    /**
     * 能耗抄表列表
     *
     * @param queryParam
     * @return
     */

    @Operation(summary = "能耗抄表列表")
    @PostMapping("/query")
    @QueryAction
    public Mono<PagerResult<EnergyMeterReturnRes>> energyMeter(@RequestBody QueryParamEntity queryParam) {
        return queryHelper
            .select("SELECT    t1.*, t2.device_name as deviceName  FROM sems_energy_meter t1 LEFT JOIN (select  distinct device_id,device_name from sems_device_info) t2  on t1.device_id=t2.device_id WHERE !ISNULL(device_name)  ORDER BY t1.this_meter_time DESC ,t2.device_name ASC",EnergyMeterReturnRes::new)
            .where(queryParam)
            .fetchPaged();
    }


    @Operation(summary = "抄表测试")
    @GetMapping("/test")
    @QueryAction
    public Mono<Void> escalation() {
        //获取昨天的数据
        //水上报

        LocalDateTime start = LocalDateTime.of(LocalDate.now().plusDays(-1), LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(LocalDate.now().plusDays(-1), LocalTime.MAX);
        log.info("昨天开始时间时间戳"+start.toInstant(ZoneOffset.of("+8")).toEpochMilli());
        log.info("昨天结束时间时间戳"+end.toInstant(ZoneOffset.of("+8")).toEpochMilli());
        return waterConsumeService.createQuery()
            .gte(WaterConsumeEntity::getGatherTime, start.toInstant(ZoneOffset.of("+8")).toEpochMilli())
            .lte(WaterConsumeEntity::getGatherTime, end.toInstant(ZoneOffset.of("+8")).toEpochMilli())
            .fetch()
            .filter(i->i.getNumber()!= null)
            .collectList()
            .flatMapMany(value -> {
                //按设备分组,并取每一条中时间最大的
                Map<String, WaterConsumeEntity> waterConsumeEntityMap = value.stream().collect(Collectors.toMap(WaterConsumeEntity::getReportDeviceId, Function.identity(), (c1, c2) -> c1.getGatherTime() > c2.getGatherTime() ? c1 : c2));


                //查询各设备上次抄表记录,最新的一条
                return service
                    .createQuery()
                    .where(EnergyMeterReadingEntity::getEnergyType, EnergyType.water)
                    .fetch()
                    .collectList()
                    .flatMap(l -> {
                        Map<String, EnergyMeterReadingEntity> collect = l.parallelStream().collect(
                            Collectors.groupingBy(EnergyMeterReadingEntity::getDeviceId, // 先根据设备id分组
                                Collectors.collectingAndThen(
                                    Collectors.reducing((c1, c2) -> c1.getThisMeterTime() > c2.getThisMeterTime() ? c1 : c2), Optional::get)));
                        ArrayList<EnergyMeterReadingEntity> result = new ArrayList<>();
                        for (Map.Entry<String, WaterConsumeEntity> entity : waterConsumeEntityMap.entrySet()) {
                            WaterConsumeEntity waterConsumeEntityValue = entity.getValue();
                            String key = entity.getKey();
                            EnergyMeterReadingEntity energyMeterReadingEntity = new EnergyMeterReadingEntity();
                            energyMeterReadingEntity.setDeviceId(waterConsumeEntityValue.getReportDeviceId());
                            energyMeterReadingEntity.setThisMeterTime(waterConsumeEntityValue.getGatherTime());
                            energyMeterReadingEntity.setThisMeterNum(waterConsumeEntityValue.getNumber()==null? BigDecimal.ZERO:waterConsumeEntityValue.getNumber());
                            energyMeterReadingEntity.setEnergyType(EnergyType.water);


                            if(collect==null || collect.get(key) ==null){
                                energyMeterReadingEntity.setLastMeterTime(null);
                                energyMeterReadingEntity.setLastMeterNum(null);
                                energyMeterReadingEntity.setDifference(BigDecimal.ZERO);
                                result.add(energyMeterReadingEntity);
                            }else {
                                energyMeterReadingEntity.setLastMeterTime(collect.get(key).getThisMeterTime());
                                energyMeterReadingEntity.setLastMeterNum(collect.get(key).getThisMeterNum());
                                energyMeterReadingEntity.setDifference(energyMeterReadingEntity.getThisMeterNum().subtract(collect.get(key).getThisMeterNum()));
                                result.add(energyMeterReadingEntity);
                            }



                        }
                        return service.save(result);
                    });
            })
            .then(this.meterElectricity())
            .then(this.meterGas());
    }

    public Mono<Void> meterElectricity(){
        LocalDateTime start = LocalDateTime.of(LocalDate.now().plusDays(-1), LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(LocalDate.now().plusDays(-1), LocalTime.MAX);
        return electricityConsumeService.createQuery()
            .gte(ElectricityConsumeEntity::getGatherTime, start.toInstant(ZoneOffset.of("+8")).toEpochMilli())
            .lte(ElectricityConsumeEntity::getGatherTime, end.toInstant(ZoneOffset.of("+8")).toEpochMilli())
            .fetch()
            .filter(i->i.getNumber()!= null)
            .collectList()
            .flatMapMany(value -> {
                //按设备分组,并取每一条中时间最大的
                Map<String, ElectricityConsumeEntity> electricityConsumeEntityMap = value.stream().collect(Collectors.toMap(ElectricityConsumeEntity::getReportDeviceId, Function.identity(), (c1, c2) -> c1.getGatherTime() > c2.getGatherTime() ? c1 : c2));

                ArrayList<EnergyMeterReadingEntity> result = new ArrayList<>();
                //查询各设备上次抄表记录,最新的一条

                return service.createQuery()
                    .where(EnergyMeterReadingEntity::getEnergyType, EnergyType.electricity)
                    .fetch()
                    .collectList()
                    .flatMap(l -> {
                        Map<String, EnergyMeterReadingEntity> collect = l.parallelStream().collect(
                            Collectors.groupingBy(EnergyMeterReadingEntity::getDeviceId, // 先根据设备id分组
                                Collectors.collectingAndThen(
                                    Collectors.reducing((c1, c2) -> c1.getThisMeterTime() > c2.getThisMeterTime() ? c1 : c2), Optional::get)));
                        for (Map.Entry<String, ElectricityConsumeEntity> entity : electricityConsumeEntityMap.entrySet()) {
                            String key = entity.getKey();
                            ElectricityConsumeEntity electricityConsumeEntity = entity.getValue();
                            EnergyMeterReadingEntity energyMeterReadingEntity = new EnergyMeterReadingEntity();
                            energyMeterReadingEntity.setDeviceId(electricityConsumeEntity.getReportDeviceId());
                            energyMeterReadingEntity.setThisMeterTime(electricityConsumeEntity.getGatherTime());
                            energyMeterReadingEntity.setThisMeterNum(electricityConsumeEntity.getNumber() == null ? BigDecimal.ZERO : electricityConsumeEntity.getNumber());
                            energyMeterReadingEntity.setEnergyType(EnergyType.electricity);

                            if (collect == null || collect.get(key) == null) {
                                energyMeterReadingEntity.setLastMeterTime(null);
                                energyMeterReadingEntity.setLastMeterNum(null);
                                energyMeterReadingEntity.setDifference(BigDecimal.ZERO);
                                result.add(energyMeterReadingEntity);
                            } else {
                                energyMeterReadingEntity.setLastMeterTime(collect.get(key).getThisMeterTime());
                                energyMeterReadingEntity.setLastMeterNum(collect.get(key).getThisMeterNum());
                                energyMeterReadingEntity.setDifference(energyMeterReadingEntity.getThisMeterNum().subtract(collect.get(key).getThisMeterNum()));
                                result.add(energyMeterReadingEntity);
                            }
                        }
                        return service.save(result);
                    });
            }).then();
    }

    public Mono<Void> meterGas(){
        LocalDateTime start = LocalDateTime.of(LocalDate.now().plusDays(-1), LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(LocalDate.now().plusDays(-1), LocalTime.MAX);
        return gasConsumeService.createQuery()
            .gte(GasConsumeEntity::getGatherTime, start.toInstant(ZoneOffset.of("+8")).toEpochMilli())
            .lte(GasConsumeEntity::getGatherTime, end.toInstant(ZoneOffset.of("+8")).toEpochMilli())
            .fetch()
            .filter(i->i.getNumber()!= null)
            .collectList()
            .flatMapMany(value -> {
                //按设备分组,并取每一条中时间最大的
                Map<String, GasConsumeEntity> gasConsumeEntityMap = value.stream().collect(Collectors.toMap(GasConsumeEntity::getReportDeviceId, Function.identity(), (c1, c2) -> c1.getGatherTime() > c2.getGatherTime() ? c1 : c2));

                ArrayList<EnergyMeterReadingEntity> result = new ArrayList<>();
                //查询各设备上次抄表记录,最新的一条

                return service.createQuery()
                    .where(EnergyMeterReadingEntity::getEnergyType,EnergyType.gas)
                    .fetch()
                    .collectList()
                    .flatMap(l -> {
                        Map<String, EnergyMeterReadingEntity> collect = l.parallelStream().collect(
                            Collectors.groupingBy(EnergyMeterReadingEntity::getDeviceId, // 先根据设备id分组
                                Collectors.collectingAndThen(
                                    Collectors.reducing((c1, c2) -> c1.getThisMeterTime() > c2.getThisMeterTime() ? c1 : c2), Optional::get)));
                        for (Map.Entry<String, GasConsumeEntity> entity : gasConsumeEntityMap.entrySet()) {
                            GasConsumeEntity gasConsumeEntity = entity.getValue();
                            String key = entity.getKey();
                            EnergyMeterReadingEntity energyMeterReadingEntity = new EnergyMeterReadingEntity();
                            energyMeterReadingEntity.setDeviceId(gasConsumeEntity.getReportDeviceId());
                            energyMeterReadingEntity.setThisMeterTime(gasConsumeEntity.getGatherTime());
                            energyMeterReadingEntity.setThisMeterNum(gasConsumeEntity.getNumber()==null?BigDecimal.ZERO:gasConsumeEntity.getNumber());
                            energyMeterReadingEntity.setEnergyType(EnergyType.gas);

                            if(collect==null  || collect.get(key )==null){
                                energyMeterReadingEntity.setLastMeterTime(null);
                                energyMeterReadingEntity.setLastMeterNum(null);
                                energyMeterReadingEntity.setDifference(BigDecimal.ZERO);
                                result.add(energyMeterReadingEntity);
                            }else {
                                energyMeterReadingEntity.setLastMeterTime(collect.get(key).getThisMeterTime());
                                energyMeterReadingEntity.setLastMeterNum(collect.get(key).getThisMeterNum());
                                energyMeterReadingEntity.setDifference(energyMeterReadingEntity.getThisMeterNum().subtract(collect.get(key).getThisMeterNum()));
                                result.add(energyMeterReadingEntity);
                            }
                        }
                        return service.save(result);
                    });

            })
            .then();
    }




    //导出数据
    @GetMapping("/_export/{name}.{format}")
    public Mono<Void> export(
                             @PathVariable String name,
                             //文件格式: 支持csv,xlsx
                             @PathVariable String format,
                             ServerWebExchange exchange) {

        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_OCTET_STREAM);
        //文件名
        exchange.getResponse().getHeaders().setContentDisposition(
            ContentDisposition
                .attachment()
                .filename(name + "." + format, StandardCharsets.UTF_8)
                .build()
        );
        return exchange
            .getResponse()
            .writeWith(
                ExcelUtils.write(EnergyMeterReturnRes.class, this.energyMeter(), format)
            );
    }

    public Flux<EnergyMeterReturnRes> energyMeter() {
        return queryHelper
            .select("SELECT    t1.*, t2.device_name as deviceName  FROM sems_energy_meter t1 LEFT JOIN (select  distinct device_id,device_name from sems_device_info) t2  on t1.device_id=t2.device_id WHERE !ISNULL(device_name)  ORDER BY t1.this_meter_time DESC ,t2.device_name ASC",EnergyMeterReturnRes::new)
            .where(new QueryParamEntity().noPaging())
            .fetch();
    }


}

