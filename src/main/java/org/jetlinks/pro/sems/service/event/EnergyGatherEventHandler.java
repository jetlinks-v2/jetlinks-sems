package org.jetlinks.pro.sems.service.event;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.jetlinks.pro.sems.entity.*;
import org.jetlinks.pro.sems.service.*;
import org.jetlinks.pro.sems.entity.res.CostconfigTimeRes;
import org.jetlinks.pro.sems.enums.EnergyType;
import org.jetlinks.pro.sems.iot.IotService;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class EnergyGatherEventHandler {

    private final CostConfService costConfService;

    private final ElectricityIntervalService electricityIntervalService;

    private final ElectricityConsumeService electricityConsumeService;

    private final GasConsumeService gasConsumeService;

    private final WaterConsumeService waterConsumeService;

    private final AlarmRecordsService alarmRecordsService;

    private final DeviceService deviceService;

    private final AlarmRecordsEventHandler alarmRecordsEventHandler;

    private final ReactiveRedisTemplate<Object, Object> redis;

    private final IotService iotService;

    @EventListener
    public void handleCreatedEvent(EntitySavedEvent<EnergyGatherEntity> event) {

        event.async(Flux.fromIterable(event.getEntity()).flatMap(this::sendSaveNotify));

    }

    public Mono<Void> sendSaveNotify(EnergyGatherEntity entity) {

        return Flux
            .just(entity)
            .flatMap(energyGatherEntity -> deviceService
                .createQuery()
                .where(DeviceInfoEntity::getDeviceId, energyGatherEntity.getDeviceId())
                .and(DeviceInfoEntity::getStatus, "0")
                .fetch()
                .collectList()
                .flatMap(list ->{
                    AtomicReference<BigDecimal> shareSize = new AtomicReference<>(BigDecimal.ZERO);
                    return Flux.fromIterable(list).flatMap(deviceEntity -> {
                        if(deviceEntity.getDeviceType().equals("0")){
                            shareSize.set(BigDecimal.valueOf(list.stream()
                                                                 .filter(e -> e.getDeviceType().equals("0")).count()));
                        }else {
                            shareSize.set(BigDecimal.valueOf(list.stream()
                                                                 .filter(e -> e.getDeviceType().equals("1")).count()));
                        }
                        JSONObject content = JSONObject.parseObject(energyGatherEntity.getContent());
                        LocalDateTime gatherTime = Instant.ofEpochMilli(energyGatherEntity.getGatherTime())
                                                          .atZone(ZoneId.systemDefault())
                                                          .toLocalDateTime();
                        if (energyGatherEntity.getEnergyType().getValue().equals("water")) {
                            WaterConsumeEntity waterEntity = new WaterConsumeEntity();
                            waterEntity.setGatherId(energyGatherEntity.getId());
                            waterEntity.setGatherTime(energyGatherEntity.getGatherTime());
                            waterEntity.setReportDeviceId(energyGatherEntity.getDeviceId());
                            waterEntity.setAreaId(deviceEntity.getAreaId());
                            if(deviceEntity.getParentFlag().equals("0")){
                                waterEntity.setDeviceId(deviceEntity.getParentId());
                            }
                            if(deviceEntity.getParentFlag().equals("1")){
                                waterEntity.setDeviceId(deviceEntity.getDeviceId());
                            }

                            if (!content.containsKey("Flow")) {
                                return Mono.empty();
                            }
                            waterEntity.setNumber(content.getBigDecimal("Flow"));

                            BigDecimal oldNumber = deviceEntity.getMeterNumber();
                            BigDecimal newNumber = waterEntity.getNumber();

                            int old = String.valueOf(oldNumber.intValue()).length();
                            int newWater = String.valueOf(waterEntity.getNumber().intValue()).length();
                            if(old > newWater){
                                BigDecimal oldMaxNumber =  new BigDecimal((int)Math.pow(10,old)).subtract(new BigDecimal(1));
                                waterEntity.setDifference(newNumber.subtract(new BigDecimal("0"))
                                                                   .add(oldMaxNumber.subtract(oldNumber))
                                                                   .divide(shareSize.get(), 3, 4));
                            } else {
                                waterEntity.setDifference(newNumber.subtract(oldNumber).divide(shareSize.get(), 3, 4));
                            }

                            //处理三种数据异常问题
                            if( waterEntity.getDifference().compareTo(BigDecimal.ZERO) < 0 ){
                                return Mono.empty();
                            }

                            return waterConsumeService
                                .save(waterEntity)
                                .flatMap(saveResult -> {
                                    if (saveResult.getTotal() > 0) {
                                        return costConfService
                                            .createQuery()
                                            .where(CostConfigEntity::getEnergyType, "1")
                                            .and(CostConfigEntity::getState, 1)
                                            .nest()
                                            .lte(CostConfigEntity::getEffectiveTimeIntervalStartDate, energyGatherEntity.getGatherTime())
                                            .gte(CostConfigEntity::getEffectiveTimeIntervalEndDate, energyGatherEntity.getGatherTime())
                                            .end()
                                            .fetch()
                                            .flatMap(costConfigEntity -> {
                                                waterEntity.setUnitPrice(BigDecimal.valueOf(costConfigEntity.getUnitPrice()));
                                                waterEntity.setCostId(costConfigEntity.getId());
                                                return waterConsumeService.updateById(waterEntity.getId(), waterEntity);
                                            })
                                            .flatMap(e->{
                                                deviceEntity.setMeterNumber(newNumber);
                                                return deviceService.save(deviceEntity);
                                            })
                                            .then(deviceService
                                                      .createQuery()
                                                      .where(DeviceInfoEntity::getDeviceId, energyGatherEntity.getDeviceId())
                                                      .and(DeviceInfoEntity::getStatus, "0")
                                                      .fetchOne()
                                                      .flatMap(deviceInfo -> deviceService
                                                          .createQuery()
                                                          .where(DeviceInfoEntity::getDeviceId, deviceInfo.getParentId())
                                                          .and(DeviceInfoEntity::getStatus, "0")
                                                          .fetchOne()
                                                          .flatMap(parentDevice -> alarmCheck(parentDevice, waterEntity,energyGatherEntity.getEnergyType()))
                                                      ));
                                    } else {
                                        return Mono.empty();
                                    }
                                })
                                .then();
                        }
                        if (energyGatherEntity.getEnergyType().getValue().equals("gas")) {
                            GasConsumeEntity gasEntity = new GasConsumeEntity();
                            gasEntity.setGatherId(energyGatherEntity.getId());
                            gasEntity.setGatherTime(energyGatherEntity.getGatherTime());
                            gasEntity.setReportDeviceId(energyGatherEntity.getDeviceId());
                            gasEntity.setAreaId(deviceEntity.getAreaId());
                            if(deviceEntity.getParentFlag().equals("0")){
                                gasEntity.setDeviceId(deviceEntity.getParentId());
                            }
                            if(deviceEntity.getParentFlag().equals("1")){
                                gasEntity.setDeviceId(deviceEntity.getDeviceId());
                            }

                            if (!content.containsKey("Flow")) {
                                return Mono.empty();
                            }
                            gasEntity.setNumber(content.getBigDecimal("Flow"));

                            BigDecimal oldNumber = deviceEntity.getMeterNumber();
                            BigDecimal newNumber = gasEntity.getNumber();

                            int old = String.valueOf(oldNumber.intValue()).length();
                            int newWater = String.valueOf(gasEntity.getNumber().intValue()).length();
                            if(old > newWater){
                                BigDecimal oldMaxNumber =  new BigDecimal((int)Math.pow(10,old)).subtract(new BigDecimal(1));
                                gasEntity.setDifference(newNumber.subtract(new BigDecimal("0"))
                                                                 .add(oldMaxNumber.subtract(oldNumber))
                                                                 .divide(shareSize.get(), 3, 4));
                            } else {
                                gasEntity.setDifference(newNumber.subtract(oldNumber).divide(shareSize.get(), 3, 4));
                            }

                            //处理三种数据异常问题
                            if( gasEntity.getDifference().compareTo(BigDecimal.ZERO) < 0 ){
                                return Mono.empty();
                            }

                            return gasConsumeService
                                .save(gasEntity)
                                .flatMap(saveResult -> {
                                    if (saveResult.getTotal() > 0) {
                                        return costConfService
                                            .createQuery()
                                            .where(CostConfigEntity::getEnergyType, "3")
                                            .and(CostConfigEntity::getState, 1)
                                            .nest()
                                            .lte(CostConfigEntity::getEffectiveTimeIntervalStartDate, energyGatherEntity.getGatherTime())
                                            .gte(CostConfigEntity::getEffectiveTimeIntervalEndDate, energyGatherEntity.getGatherTime())
                                            .end()
                                            .fetch()
                                            .flatMap(e -> {
                                                gasEntity.setUnitPrice(BigDecimal.valueOf(e.getUnitPrice()));
                                                gasEntity.setCostId(e.getId());
                                                return gasConsumeService.updateById(gasEntity.getId(), gasEntity);
                                            })
                                            .flatMap(e->{
                                                deviceEntity.setMeterNumber(newNumber);
                                                return deviceService.save(deviceEntity);
                                            })
                                            .then(deviceService
                                                      .createQuery()
                                                      .where(DeviceInfoEntity::getDeviceId, energyGatherEntity.getDeviceId())
                                                      .and(DeviceInfoEntity::getStatus, "0")
                                                      .fetchOne()
                                                      .flatMap(deviceInfo -> deviceService
                                                          .createQuery()
                                                          .where(DeviceInfoEntity::getDeviceId, deviceInfo.getParentId())
                                                          .and(DeviceInfoEntity::getStatus, "0")
                                                          .fetchOne()
                                                          .flatMap(parentDevice -> alarmCheck(parentDevice, gasEntity,energyGatherEntity.getEnergyType()))
                                                      ));
                                    } else {
                                        return Mono.empty();
                                    }
                                })
                                .then();
                        }
                        if (energyGatherEntity.getEnergyType().getValue().equals("electricity")) {
                            ElectricityConsumeEntity electricity = new ElectricityConsumeEntity();
                            electricity.setGatherId(energyGatherEntity.getId());
                            electricity.setGatherTime(energyGatherEntity.getGatherTime());
                            electricity.setReportDeviceId(energyGatherEntity.getDeviceId());
                            electricity.setAreaId(deviceEntity.getAreaId());
                            if(deviceEntity.getParentFlag().equals("0")){
                                electricity.setDeviceId(deviceEntity.getParentId());
                            }
                            if(deviceEntity.getParentFlag().equals("1")){
                                electricity.setDeviceId(deviceEntity.getDeviceId());
                            }

                            if(electricity.getDeviceId().equals("C0801008")){
                                Object auto = iotService.getDevicePropertyCurrentValue("C0801008", "manual_tower_fan_auto");
                                if(auto.equals(true)){
                                    electricity.setDeviceRunStatus("2");
                                } else {
                                    electricity.setDeviceRunStatus("1");
                                }
                            }

                            if(!content.containsKey("positiveActE") && !content.containsKey("activeP_total")){
                                return Mono.empty();
                            }

                            //电流
                            if (content.containsKey("phaseIA")) {
                                electricity.setPhaseIA(content.getBigDecimal("phaseIA"));
                            }
                            if (content.containsKey("phaseIB")) {
                                electricity.setPhaseIB(content.getBigDecimal("phaseIB"));
                            }
                            if (content.containsKey("phaseIC")) {
                                electricity.setPhaseIC(content.getBigDecimal("phaseIC"));
                            }
                            //电压
                            if (content.containsKey("phaseUA")) {
                                electricity.setPhaseUA(content.getBigDecimal("phaseUA"));
                            }
                            if (content.containsKey("phaseUB")) {
                                electricity.setPhaseUB(content.getBigDecimal("phaseUB"));
                            }
                            if (content.containsKey("phaseUC")) {
                                electricity.setPhaseUC(content.getBigDecimal("phaseUC"));
                            }
                            //有功功率
                            if (content.containsKey("activePA")) {
                                electricity.setActivePA(content.getBigDecimal("activePA"));
                            }
                            if (content.containsKey("activePB")) {
                                electricity.setActivePB(content.getBigDecimal("activePB"));
                            }
                            if (content.containsKey("activePC")) {
                                electricity.setActivePC(content.getBigDecimal("activePC"));
                            }
                            //无功功率
                            if (content.containsKey("reactivePA")) {
                                electricity.setReactivePA(content.getBigDecimal("reactivePA"));
                            }
                            if (content.containsKey("reactivePB")) {
                                electricity.setReactivePB(content.getBigDecimal("reactivePB"));
                            }
                            if (content.containsKey("reactivePC")) {
                                electricity.setReactivePC(content.getBigDecimal("reactivePC"));
                            }
                            if (content.containsKey("reactiveP_total")) {
                                electricity.setReactivePTotal(content.getBigDecimal("reactiveP_total"));
                            }
                            //频率
                            if (content.containsKey("frequency")) {
                                electricity.setFrequency(content.getBigDecimal("frequency"));
                            }
                            //总功率因数

                            if(!content.containsKey("apparentP_total") || content.getString("apparentP_total").equals("0.0")){
                                electricity.setPowerFactorTotal(BigDecimal.ZERO);
                            } else {
                                electricity.setPowerFactorTotal(content.getBigDecimal("activeP_total")
                                                                       .divide(content.getBigDecimal("apparentP_total"),2));
                            }

                            //三相电压不平衡度
                            if (content.containsKey("phaseUA") && content.containsKey("phaseUB") && content.containsKey("phaseUC")) {
                                electricity.setThreeVoltage(getThreeVoltage(electricity.getPhaseUA(),
                                                                            electricity.getPhaseUB(),
                                                                            electricity.getPhaseUC()));
                            }else {
                                electricity.setThreeVoltage(BigDecimal.ZERO);
                            }


                            //三相电流不平衡度
                            if (content.containsKey("phaseIA") && content.containsKey("phaseIB") && content.containsKey("phaseIC")) {
                                electricity.setThreePhase(getThreePhase(electricity.getPhaseIA(),
                                                                        electricity.getPhaseIB(),
                                                                        electricity.getPhaseIC()));
                            }else {
                                electricity.setThreePhase(BigDecimal.ZERO);
                            }
                            if(content.containsKey("activeP_total") && !content.getString("activeP_total").equals("0.0")){
                                electricity.setPower(content.getBigDecimal("activeP_total").divide(BigDecimal.valueOf(1000), 5, 6));
                            }
                            else {
                                electricity.setPower(BigDecimal.ZERO);
                            }
                            if (content.containsKey("positiveActE")) {
                                electricity.setNumber(content.getBigDecimal("positiveActE"));
                            }
                            BigDecimal oldNumber = deviceEntity.getMeterNumber();
                            BigDecimal newNumber = electricity.getNumber();

                            int old = String.valueOf(oldNumber.intValue()).length();
                            int newWater = String.valueOf(electricity.getNumber().intValue()).length();
                            if(old > newWater){
                                BigDecimal oldMaxNumber =  new BigDecimal((int)Math.pow(10,old)).subtract(new BigDecimal(1));
                                electricity.setDifference(newNumber.subtract(new BigDecimal("0"))
                                                                   .add(oldMaxNumber.subtract(oldNumber))
                                                                   .divide(shareSize.get(), 3, 4));
                            } else {
                                electricity.setDifference(newNumber.subtract(oldNumber).divide(shareSize.get(), 3, 4));
                            }

                            //处理三种数据异常问题
                            if( electricity.getDifference().compareTo(BigDecimal.ZERO) < 0 ){
                                return Mono.empty();
                            }

                            return electricityConsumeService
                                .save(electricity)
                                .flatMap(saveResult -> {
                                    if (saveResult.getTotal() > 0) {
                                        return costConfService
                                            .createQuery()
                                            .where(CostConfigEntity::getEnergyType, "2")
                                            .and(CostConfigEntity::getState, 1)
                                            .fetch()
                                            .flatMap(e -> electricityIntervalService
                                                .createQuery()
                                                .where(ElectricityIntervalEntity::getCostConfigId, e.getId())
                                                .and(ElectricityIntervalEntity::getState, 1)
                                                .nest()
                                                .lte(ElectricityIntervalEntity::getYearStart, gatherTime.getYear())
                                                .gte(ElectricityIntervalEntity::getYearEnd, gatherTime.getYear())
                                                .end()
                                                .$like$(ElectricityIntervalEntity::getMonth, gatherTime.getMonthValue())
                                                .fetch()
                                                .flatMap(electricityInterval -> {
                                                    BigDecimal unitPrice = new BigDecimal(e.getReferencePrice());
                                                    //尖
                                                    List<CostconfigTimeRes> cuspPeriods = electricityInterval.getCuspPeriods();
                                                    if (getTimeInterval(gatherTime, cuspPeriods)) {
                                                        List<String> reference = Arrays.asList(e.getReferenceElectricityPriceFloat()
                                                                                                .split(","));
                                                        if (reference.contains(String.valueOf(gatherTime.getMonthValue()))) {
                                                            electricity.setUnitPrice(unitPrice.add(
                                                                unitPrice.multiply(new BigDecimal(e.getReferencePriceFloat())
                                                                                       .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))));
                                                        } else {
                                                            electricity.setUnitPrice(unitPrice.add(
                                                                unitPrice.multiply(new BigDecimal(e.getOtherMonthFloat())
                                                                                       .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))));
                                                        }
                                                        electricity.setPeriodsType(1);
                                                        electricity.setCostId(electricityInterval.getCostConfigId());
                                                    }
                                                    //高
                                                    List<CostconfigTimeRes> peakPeriods = electricityInterval.getPeakPeriods();
                                                    if (getTimeInterval(gatherTime, peakPeriods)) {
                                                        electricity.setPeriodsType(2);
                                                        electricity.setCostId(electricityInterval.getCostConfigId());
                                                        electricity.setUnitPrice(unitPrice.add(
                                                            unitPrice.multiply(new BigDecimal(e.getPeakOnReferenceFloat())
                                                                                   .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))));
                                                    }
                                                    //平
                                                    List<CostconfigTimeRes> flatPeriods = electricityInterval.getFlatPeriods();
                                                    if (getTimeInterval(gatherTime, flatPeriods)) {
                                                        electricity.setPeriodsType(3);
                                                        electricity.setCostId(electricityInterval.getCostConfigId());
                                                        electricity.setUnitPrice(unitPrice);
                                                    }
                                                    //谷
                                                    List<CostconfigTimeRes> valleyPeriods = electricityInterval.getValleyPeriods();
                                                    if (getTimeInterval(gatherTime, valleyPeriods)) {
                                                        electricity.setPeriodsType(4);
                                                        electricity.setCostId(electricityInterval.getCostConfigId());
                                                        electricity.setUnitPrice(unitPrice.subtract(
                                                            unitPrice.multiply(new BigDecimal(e.getLowOnReferenceFloat())
                                                                                   .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))));
                                                    }
                                                    return electricityConsumeService.updateById(electricity.getId(), electricity);
                                                })

                                            )
                                            .flatMap(e->{
                                                deviceEntity.setMeterNumber(newNumber);
                                                return deviceService.save(deviceEntity);
                                            })
                                            .then(deviceService
                                                      .createQuery()
                                                      .where(DeviceInfoEntity::getDeviceId, energyGatherEntity.getDeviceId())
                                                      .and(DeviceInfoEntity::getStatus, "0")
                                                      .fetchOne()
                                                      .flatMap(deviceInfo -> deviceService
                                                          .createQuery()
                                                          .where(DeviceInfoEntity::getDeviceId, deviceInfo.getParentId())
                                                          .and(DeviceInfoEntity::getStatus, "0")
                                                          .fetchOne()
                                                          .flatMap(parentDevice ->
                                                                       alarmCheck(parentDevice, electricity, energyGatherEntity.getEnergyType()))
                                                      )
                                            );
                                    } else {
                                        return Mono.empty();
                                    }
                                })
                                .then();
                        }
                        return Mono.empty();
                    }).then();
                })
            ).then();
    }

    private Boolean getTimeInterval(LocalDateTime gatherTime, List<CostconfigTimeRes> timeResList) {
        boolean timeStatus = false;
        LocalTime time = LocalTime.of(gatherTime.getHour(), gatherTime.getMinute(), gatherTime.getSecond());
        for (CostconfigTimeRes timeRes : timeResList) {
            List<Integer> startDate = Arrays.stream(timeRes.getStartDate().split(":"))
                                            .map(Integer::parseInt)
                                            .collect(Collectors.toList());
            List<Integer> endDate = Arrays.stream(timeRes.getEndDate().split(":"))
                                          .map(Integer::parseInt)
                                          .collect(Collectors.toList());
            LocalTime startTime = LocalTime.of(startDate.get(0),
                                               startDate.get(1),
                                               startDate.get(2));
            LocalTime endTime = LocalTime.of(endDate.get(0),
                                             endDate.get(1),
                                             endDate.get(2));
            if(endTime.compareTo(LocalTime.of(23,59,59)) == 0){
                if (startTime.compareTo(time) <= 0 && time.compareTo(endTime) <= 0) {
                    timeStatus = true;
                    break;
                }
            } else {
                if (startTime.compareTo(time) <= 0 && time.compareTo(endTime) < 0) {
                    timeStatus = true;
                    break;
                }
            }
        }
        return timeStatus;

    }

    private Mono<Void> alarmCheck(DeviceInfoEntity parentDevice, Object entity, EnergyType energyType) {
        //设备告警
        String deviceRedisEnergy = "Rule:" + parentDevice.getDeviceId() + "0" +  energyType + "0";
        String deviceRedisPower = "Rule:" + parentDevice.getDeviceId() + "0" +  energyType + "1";
        //场所告警
        String placeRedis = "Rule:" + parentDevice.getPlaceId() + "2" + energyType;

        return redis
            .hasKey(deviceRedisEnergy)
            .flatMap(hasRedis -> {
                if (hasRedis) {
                    return redis
                        .opsForValue()
                        .get(deviceRedisEnergy)
                        .flatMap(alarmRule -> {
                            AlarmRuleEntity alarmRuleEntity = (AlarmRuleEntity) alarmRule;
                            if(alarmRuleEntity.getRuleStatus().equals("0")){
                                return Mono.empty();
                            }
                            JSONObject consumeEntity = JSONObject.parseObject(JSON.toJSONString(entity));
                            return alarmRecordsEventHandler.energyValueRuleCheck(alarmRuleEntity,consumeEntity);

                        });
                } else {
                    return Mono.empty();
                }
            })
            .then(
                redis
                    .hasKey(deviceRedisPower)
                    .flatMap(hasRedis -> {
                        if (hasRedis) {
                            return redis
                                .opsForValue()
                                .get(deviceRedisPower)
                                .flatMap(alarmRule -> {
                                    AlarmRuleEntity alarmRuleEntity = (AlarmRuleEntity) alarmRule;
                                    if(alarmRuleEntity.getRuleStatus().equals("0")){
                                        return Mono.empty();
                                    }
                                    JSONObject consumeEntity = JSONObject.parseObject(JSON.toJSONString(entity));
                                    return alarmRecordsEventHandler.powerRuleCheck(alarmRuleEntity,consumeEntity);
                                });
                        } else {
                            return Mono.empty();
                        }
                    })
            ).then(
                redis
                    .hasKey(placeRedis)
                    .flatMap(hasRedis -> {
                        if (hasRedis) {
                            return redis
                                .opsForValue()
                                .get(placeRedis)
                                .flatMap(alarmRule -> {
                                    AlarmRuleEntity alarmRuleEntity = (AlarmRuleEntity) alarmRule;
                                    if(alarmRuleEntity.getRuleStatus().equals("0")){
                                        return Mono.empty();
                                    }
                                    JSONObject consumeEntity = JSONObject.parseObject(JSON.toJSONString(entity));
                                    return alarmRecordsEventHandler.placeRuleCheck(alarmRuleEntity,consumeEntity);
                                });
                        } else {
                            return Mono.empty();
                        }
                    })
            );
    }

    //计算三相电压不平衡度
    public BigDecimal getThreeVoltage(BigDecimal A,BigDecimal B,BigDecimal C){
        BigDecimal avg = (A.add(B).add(C)).divide(BigDecimal.valueOf(3),2);
        BigDecimal max = A.max(B).max(C);
        BigDecimal min = A.min(B).min(C);
        if(avg.compareTo(BigDecimal.ZERO) == 0){
            return BigDecimal.ZERO;
        }else {
            return max.subtract(min).divide(avg,4,5).multiply(BigDecimal.valueOf(100));
        }
    }

    //计算三相电流不平衡度
    public BigDecimal getThreePhase(BigDecimal A,BigDecimal B,BigDecimal C){
        BigDecimal avg = A.add(B).add(C).divide(BigDecimal.valueOf(3),2);
        BigDecimal APhase = A.subtract(avg).abs();
        BigDecimal BPhase = B.subtract(avg).abs();
        BigDecimal CPhase = C.subtract(avg).abs();
        BigDecimal max = APhase.max(BPhase).max(CPhase);
        if(avg.compareTo(BigDecimal.ZERO) == 0){
            return BigDecimal.ZERO;
        }else {
            return max.divide(avg,2);
        }
    }
}
