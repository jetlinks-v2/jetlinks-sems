package org.jetlinks.project.busi.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.crud.events.*;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.project.busi.entity.*;
import org.jetlinks.project.busi.entity.res.CostconfigTimeRes;
import org.jetlinks.project.busi.service.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class CostConfiEventHandler {

    private final QueryHelper queryHelper;

    private final ElectricityConsumeService electricityConsumeService;

    private final WaterConsumeService waterConsumeService;

    private final GasConsumeService gasConsumeService;

    private final ElectricityIntervalService electricityIntervalService;

    private final CostConfService costConfService;

    private final OperateLogService operateLogService;

    @EventListener
    public void handleUpdateEvent(EntityModifyEvent<CostConfigEntity> event) {

        event.async(this.sendUpdateNotify(event.getAfter()));
    }

    @EventListener
    public void handleInsertEvent(EntityCreatedEvent<CostConfigEntity> event) {

        event.async(this.sendInsertNotify(event.getEntity()));
    }

    @EventListener
    public void handleBeforeUpdateEvent(EntityBeforeModifyEvent<CostConfigEntity> event) {

        event.async(this.sendBeforeUpdateNotify(event.getAfter()));
    }

    @EventListener
    public void handleBeforeSaveEvent(EntityBeforeSaveEvent<CostConfigEntity> event) {

        event.async(this.sendBeforeSaveNotify(event.getEntity()));
    }

    public Mono<Void> sendInsertNotify(List<CostConfigEntity> costConfigList){
        Flux.fromIterable(costConfigList)
            .flatMap(costConfig -> costConfService
                       .createQuery()
                       .where(CostConfigEntity::getState,1)
                       .and(CostConfigEntity::getEnergyType,costConfig.getEnergyType())
                       .fetch()
                       .collectList()
                       .flatMap(list ->{
                           if(list.size() > 0){
                               if("2".equals(costConfig.getEnergyType())){
                                   return Flux.fromIterable(costConfig.getElectricityIntervalEntities())
                                              .flatMap(electricityInterval -> {
                                                  String[] monthList;
                                                  if(electricityInterval.getMonth().contains(",")){
                                                      monthList = electricityInterval.getMonth().split(",");
                                                  } else {
                                                      monthList = new String[]{electricityInterval.getMonth()};
                                                  }
                                                  return queryHelper
                                                      .select("SELECT \n" +
                                                                  "* \n" +
                                                                  "FROM `sems_electricity_consume`  \n" +
                                                                  "WHERE year(FROM_UNIXTIME(ROUND(gather_time/1000,0))) BETWEEN ? and ? \n" +
                                                                  "and month(FROM_UNIXTIME(ROUND(gather_time/1000,0))) in (?)", ElectricityConsumeEntity::new,
                                                              electricityInterval.getYearStart(),electricityInterval.getYearEnd(),monthList)
                                                      .fetch()
                                                      .flatMap(electricity -> electricityCost(electricity,electricityInterval,costConfig));
                                              }).then();
                               }else {
                                   //水和气
                                   if("1".equals(costConfig.getEnergyType())){
                                       return waterConsumeService
                                           .createQuery()
                                           .gte(WaterConsumeEntity::getGatherTime,costConfig.getEffectiveTimeIntervalStartDate())
                                           .lte(WaterConsumeEntity::getGatherTime,costConfig.getEffectiveTimeIntervalEndDate())
                                           .fetch()
                                           .flatMap(water -> {
                                               water.setUnitPrice(BigDecimal.valueOf(costConfig.getUnitPrice()));
                                               water.setCostId(costConfig.getId());
                                               return waterConsumeService.updateById(water.getId(), water);
                                           }).then();
                                   }else {
                                       return gasConsumeService
                                           .createQuery()
                                           .gte(GasConsumeEntity::getGatherTime,costConfig.getEffectiveTimeIntervalStartDate())
                                           .lte(GasConsumeEntity::getGatherTime,costConfig.getEffectiveTimeIntervalEndDate())
                                           .fetch()
                                           .flatMap(gas -> {
                                               gas.setUnitPrice(BigDecimal.valueOf(costConfig.getUnitPrice()));
                                               gas.setCostId(costConfig.getId());
                                               return gasConsumeService.updateById(gas.getId(), gas);
                                           }).then();
                                   }
                               }
                           } else {
                               if("2".equals(costConfig.getEnergyType())){
                                   return Flux.fromIterable(costConfig.getElectricityIntervalEntities())
                                              .flatMap(electricityInterval ->
                                                           electricityConsumeService
                                                               .createQuery()
                                                               .fetch()
                                                               .flatMap(electricity -> electricityCost(electricity,electricityInterval,costConfig)))
                                              .then();
                               }else {
                                   //水和气
                                   if("1".equals(costConfig.getEnergyType())){
                                       return waterConsumeService
                                           .createQuery()
                                           .fetch()
                                           .flatMap(water -> {
                                               water.setUnitPrice(BigDecimal.valueOf(costConfig.getUnitPrice()));
                                               water.setCostId(costConfig.getId());
                                               return waterConsumeService.updateById(water.getId(), water);
                                           })
                                           .then();
                                   }else {
                                       return gasConsumeService
                                           .createQuery()
                                           .fetch()
                                           .flatMap(gas -> {
                                               gas.setUnitPrice(BigDecimal.valueOf(costConfig.getUnitPrice()));
                                               gas.setCostId(costConfig.getId());
                                               return gasConsumeService.updateById(gas.getId(), gas);
                                           })
                                           .then();
                                   }
                               }

                           }
                       })
                   ).subscribe();
                   return Mono.empty();
    }

    public Mono<Void> sendUpdateNotify(List<CostConfigEntity> costConfigEntities){
        Flux.fromIterable(costConfigEntities)
            .flatMap(costConfig ->{
                if("2".equals(costConfig.getEnergyType())){
                    return electricityIntervalService
                        .createQuery()
                        .where(ElectricityIntervalEntity::getCostConfigId, costConfig.getId())
                        .and(ElectricityIntervalEntity::getState, 1)
                        .fetch()
                        .flatMap(electricityInterval ->
                                     electricityConsumeService
                                         .createQuery()
                                         .where(ElectricityConsumeEntity::getCostId,costConfig.getId())
                                         .fetch()
                                         .flatMap(electricity -> electricityCost(electricity,electricityInterval,costConfig)));
                }else {
                    if ("1".equals(costConfig.getEnergyType())) {
                        return waterConsumeService
                            .createQuery()
                            .where(WaterConsumeEntity::getCostId, costConfig.getId())
                            .fetch()
                            .flatMap(water -> {
                                water.setUnitPrice(BigDecimal.valueOf(costConfig.getUnitPrice()));
                                water.setCostId(costConfig.getId());
                                return waterConsumeService.updateById(water.getId(), water);
                            });
                    } else {
                        return gasConsumeService
                            .createQuery()
                            .where(GasConsumeEntity::getCostId, costConfig.getId())
                            .fetch()
                            .flatMap(gas -> {
                                gas.setUnitPrice(BigDecimal.valueOf(costConfig.getUnitPrice()));
                                gas.setCostId(costConfig.getId());
                                return gasConsumeService.updateById(gas.getId(), gas);
                            });
                    }
                }
            }).subscribe();
         return Mono.empty();
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

    private Mono<Integer> electricityCost(ElectricityConsumeEntity electricity, ElectricityIntervalEntity electricityInterval, CostConfigEntity costConfig){
        LocalDateTime gatherTime = Instant.ofEpochMilli(electricity.getGatherTime())
                                          .atZone(ZoneId.systemDefault())
                                          .toLocalDateTime();
        BigDecimal unitPrice = new BigDecimal(costConfig.getReferencePrice());
        //尖
        List<CostconfigTimeRes> cuspPeriods = electricityInterval.getCuspPeriods();
        if (getTimeInterval(gatherTime, cuspPeriods)) {
            List<String> reference = Arrays.asList(costConfig.getReferenceElectricityPriceFloat()
                                                             .split(","));
            if (reference.contains(String.valueOf(gatherTime.getMonthValue()))) {
                electricity.setUnitPrice(unitPrice.add(
                    unitPrice.multiply(new BigDecimal(costConfig.getReferencePriceFloat())
                                           .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))));
            } else {
                electricity.setUnitPrice(unitPrice.add(
                    unitPrice.multiply(new BigDecimal(costConfig.getOtherMonthFloat())
                                           .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))));
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
                unitPrice.multiply(new BigDecimal(costConfig.getPeakOnReferenceFloat())
                                       .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))));
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
                unitPrice.multiply(new BigDecimal(costConfig.getLowOnReferenceFloat())
                                       .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))));
        }
        return electricityConsumeService.updateById(electricity.getId(), electricity);
    }

    public Mono<Void> sendBeforeUpdateNotify(List<CostConfigEntity> costConfigList){

        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))//如果没有用户信息则抛出异常
            .flatMap(autz-> Flux
                .fromIterable(costConfigList)
                .flatMap(costConfig -> costConfService
                    .findById(costConfig.getId())
                    .flatMap(costEntity ->{

                        OperateLogEntity logEntity = new OperateLogEntity();
                        logEntity.setFuncModule("费用配置");
                        logEntity.setJobNumber(autz.getUser().getUsername());
                        logEntity.setOperateUser(autz.getUser().getName());
                        if ("1".equals(costConfig.getEnergyType()) && !costEntity.getUnitPrice().equals(costConfig.getUnitPrice())){
                            logEntity.setOperateContent("水-单价:" + costEntity.getUnitPrice() + "改为" + costConfig.getUnitPrice());
                            return operateLogService.insert(logEntity);
                        }
                        if ("2".equals(costConfig.getEnergyType()) && !costEntity.getReferencePrice().equals(costConfig.getReferencePrice())){
                            logEntity.setOperateContent("电-基准电价:" + costEntity.getReferencePrice() + "改为" + costConfig.getReferencePrice());
                            return operateLogService.insert(logEntity);
                        }
                        if ("3".equals(costConfig.getEnergyType()) && !costEntity.getUnitPrice().equals(costConfig.getUnitPrice())){
                            logEntity.setOperateContent("气-单价:" + costEntity.getUnitPrice() + "改为" + costConfig.getUnitPrice());
                            return operateLogService.insert(logEntity);
                        }
                        return Mono.empty();
                    })).then()
            );
    }

    public Mono<Void> sendBeforeSaveNotify(List<CostConfigEntity> costConfigList){

        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))//如果没有用户信息则抛出异常
            .flatMap(autz-> Flux
                .fromIterable(costConfigList)
                .flatMap(costConfig -> costConfService
                    .findById(costConfig.getId())
                    .switchIfEmpty(Mono.just(costConfig))
                    .flatMap(costEntity ->{
                        if(Objects.nonNull(costEntity.getId())){
                            OperateLogEntity logEntity = new OperateLogEntity();
                            logEntity.setFuncModule("费用配置");
                            logEntity.setJobNumber(autz.getUser().getUsername());
                            logEntity.setOperateUser(autz.getUser().getName());
                            if ("1".equals(costConfig.getEnergyType()) && !costEntity.getUnitPrice().equals(costConfig.getUnitPrice())){
                                logEntity.setOperateContent("水-单价:" + costEntity.getUnitPrice() + "改为" + costConfig.getUnitPrice());
                                return operateLogService.insert(logEntity);
                            }
                            if ("2".equals(costConfig.getEnergyType()) && !costEntity.getReferencePrice().equals(costConfig.getReferencePrice())){
                                logEntity.setOperateContent("电-基准电价:" + costEntity.getReferencePrice() + "改为" + costConfig.getReferencePrice());
                                return operateLogService.insert(logEntity);
                            }
                            if ("3".equals(costConfig.getEnergyType()) && !costEntity.getUnitPrice().equals(costConfig.getUnitPrice())){
                                logEntity.setOperateContent("气-单价:" + costEntity.getUnitPrice() + "改为" + costConfig.getUnitPrice());
                                return operateLogService.insert(logEntity);
                            }
                        }
                        return Mono.empty();
                    })).then()
            );
    }

}
