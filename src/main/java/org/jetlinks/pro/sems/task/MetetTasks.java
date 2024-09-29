package org.jetlinks.pro.sems.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.pro.TimerSpec;
import org.jetlinks.pro.cluster.reactor.FluxCluster;
import org.jetlinks.pro.sems.entity.ElectricityConsumeEntity;
import org.jetlinks.pro.sems.entity.EnergyMeterReadingEntity;
import org.jetlinks.pro.sems.entity.GasConsumeEntity;
import org.jetlinks.pro.sems.entity.WaterConsumeEntity;
import org.jetlinks.pro.sems.enums.EnergyType;
import org.jetlinks.pro.sems.service.ElectricityConsumeService;
import org.jetlinks.pro.sems.service.EnergyMeterReadingService;
import org.jetlinks.pro.sems.service.GasConsumeService;
import org.jetlinks.pro.sems.service.WaterConsumeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j

public class MetetTasks implements CommandLineRunner {




    private final WaterConsumeService waterConsumeService;

    private final ElectricityConsumeService electricityConsumeService;

    private final GasConsumeService gasConsumeService;

    private final EnergyMeterReadingService service;

    public Mono<Void> escalation() {
        //获取昨天的数据
        //水上报

        Long start = LocalDateTime.of(LocalDate.now().plusDays(-1), LocalTime.MIN).toInstant(ZoneOffset.of("+8")).toEpochMilli();
        Long end = LocalDateTime.of(LocalDate.now().plusDays(-1), LocalTime.MAX).toInstant(ZoneOffset.of("+8")).toEpochMilli();



        return waterConsumeService.createQuery()
            .gte(WaterConsumeEntity::getGatherTime, start)
            .lte(WaterConsumeEntity::getGatherTime, end)
           // .not(WaterConsumeEntity::getDeviceId,"0")
            .fetch()
            .collectList()
            .flatMap(value -> {
                //按设备分组,并取每一条中时间最大的
                Map<String, WaterConsumeEntity> waterConsumeEntityMap = value.stream().collect(Collectors.toMap(WaterConsumeEntity::getReportDeviceId, Function.identity(), (c1, c2) -> c1.getGatherTime() >= c2.getGatherTime() ? c1 : c2));


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
                                    Collectors.reducing((c1, c2) -> c1.getThisMeterTime() >= c2.getThisMeterTime() ? c1 : c2), Optional::get)));
                        ArrayList<EnergyMeterReadingEntity> result = new ArrayList<>();
                        for (Map.Entry<String, WaterConsumeEntity> entity : waterConsumeEntityMap.entrySet()) {
                            WaterConsumeEntity waterConsumeEntityValue = entity.getValue();
                            String key = entity.getKey();
                            EnergyMeterReadingEntity energyMeterReadingEntity = new EnergyMeterReadingEntity();
                            energyMeterReadingEntity.setDeviceId(waterConsumeEntityValue.getReportDeviceId());
                            energyMeterReadingEntity.setThisMeterTime(waterConsumeEntityValue.getGatherTime());
                            energyMeterReadingEntity.setThisMeterNum(waterConsumeEntityValue.getNumber()==null?BigDecimal.ZERO:waterConsumeEntityValue.getNumber());
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
            .then(this.meterElectricity(start,end))
            .then(this.meterGas(start,end));
    }

    public Mono<Void> meterElectricity(Long start ,Long end){
        return electricityConsumeService.createQuery()
            .gte(ElectricityConsumeEntity::getGatherTime, start)
            .lte(ElectricityConsumeEntity::getGatherTime, end)
           // .not(WaterConsumeEntity::getDeviceId,"0")
            .fetch()
            .collectList()
            .flatMap(value -> {
                //按设备分组,并取每一条中时间最大的
                Map<String, ElectricityConsumeEntity> electricityConsumeEntityMap = value.stream().collect(Collectors.toMap(ElectricityConsumeEntity::getReportDeviceId, Function.identity(), (c1, c2) -> c1.getGatherTime() >= c2.getGatherTime() ? c1 : c2));

                ArrayList<EnergyMeterReadingEntity> result = new ArrayList<>();
                //查询各设备上次抄表记录,最新的一条

                return service.createQuery()
                    .where(EnergyMeterReadingEntity::getEnergyType, EnergyType.electricity)
                    .fetch()
                    .collectList()
                    .flatMap(l -> {
                        Map<String, EnergyMeterReadingEntity> collect = l.stream().collect(
                            Collectors.groupingBy(EnergyMeterReadingEntity::getDeviceId, // 先根据设备id分组
                                Collectors.collectingAndThen(
                                    Collectors.reducing((c1, c2) -> c1.getThisMeterTime() >= c2.getThisMeterTime() ? c1 : c2), Optional::get)));
                        for (Map.Entry<String, ElectricityConsumeEntity> entity : electricityConsumeEntityMap.entrySet()) {
                            String key = entity.getKey();
                            ElectricityConsumeEntity electricityConsumeEntity = entity.getValue();
                            EnergyMeterReadingEntity energyMeterReadingEntity = new EnergyMeterReadingEntity();
                            energyMeterReadingEntity.setDeviceId(electricityConsumeEntity.getReportDeviceId());
                            energyMeterReadingEntity.setThisMeterTime(electricityConsumeEntity.getGatherTime());
                            energyMeterReadingEntity.setThisMeterNum(electricityConsumeEntity.getNumber() == null ? BigDecimal.ZERO : electricityConsumeEntity.getNumber());
                            energyMeterReadingEntity.setEnergyType(EnergyType.electricity);

                            if (collect == null  || collect.get(key) == null) {
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

    public Mono<Void> meterGas(Long start,Long end){
        return gasConsumeService.createQuery()
            .gte(GasConsumeEntity::getGatherTime, start)
            .lte(GasConsumeEntity::getGatherTime, end)
            //.not(WaterConsumeEntity::getDeviceId,"0")
            .fetch()
            .collectList()
            .flatMap(value -> {
                //按设备分组,并取每一条中时间最大的
                Map<String, GasConsumeEntity> gasConsumeEntityMap = value.stream().collect(Collectors.toMap(GasConsumeEntity::getReportDeviceId, Function.identity(), (c1, c2) -> c1.getGatherTime() >= c2.getGatherTime() ? c1 : c2));

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
                                    Collectors.reducing((c1, c2) -> c1.getThisMeterTime() >= c2.getThisMeterTime() ? c1 : c2), Optional::get)));
                        for (Map.Entry<String, GasConsumeEntity> entity : gasConsumeEntityMap.entrySet()) {
                            GasConsumeEntity gasConsumeEntity = entity.getValue();
                            String key = entity.getKey();
                            EnergyMeterReadingEntity energyMeterReadingEntity = new EnergyMeterReadingEntity();
                            energyMeterReadingEntity.setDeviceId(gasConsumeEntity.getReportDeviceId());
                            energyMeterReadingEntity.setThisMeterTime(gasConsumeEntity.getGatherTime());
                            energyMeterReadingEntity.setThisMeterNum(gasConsumeEntity.getNumber()==null?BigDecimal.ZERO:gasConsumeEntity.getNumber());
                            energyMeterReadingEntity.setEnergyType(EnergyType.gas);

                            if(collect==null || collect.get(key )==null){
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

    private Disposable disposable;

    @PreDestroy
    public void shutdown() {
        //停止定时任务
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void run(String... args) throws Exception {
        disposable =
            FluxCluster
                //不同的任务名不能相同
                .schedule("meter_task", TimerSpec.cron("59 0 0 * * ?"),Mono.defer(this::escalation));
    }
}
