package org.jetlinks.pro.sems.service.event;

import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.sems.entity.*;
import org.jetlinks.pro.sems.service.TestConfigDeviceService;
import org.jetlinks.pro.sems.service.TestEnergyDetailService;
import org.jetlinks.pro.sems.service.TestRecordService;
import org.jetlinks.pro.sems.entity.res.EnergyDayRes;
//import org.jetlinks.project.busi.task.TestEnergyTask;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
@RequiredArgsConstructor
public class TestRecordEventHandler {

    private final TestRecordService service;

    private final TestConfigDeviceService configDeviceService;

    private final TestEnergyDetailService testEnergyDetailService;

    private final QueryHelper queryHelper;

    private final AlarmRecordsEventHandler alarmRecordsEventHandler;

    private final ReactiveRedisTemplate<Object, Object> redis;

    private static HashMap<String, Disposable> disposableHashMap = new HashMap<>();

    @EventListener
    public void handleCreatedEvent(EntityCreatedEvent<TestRecordEntity> event) {

        event.async(this.sendCreatedNotify(event.getEntity()));

    }

    public Mono<Void> sendCreatedNotify(List<TestRecordEntity> testRecordList) {
        //新增试验记录时，添加相关试验设备能耗记录
        return Flux
            .fromIterable(testRecordList)
            .flatMap(entity -> configDeviceService
                .createQuery()
                .where(TestConfigDeviceEntity::getConfigId, entity.getConfigId())
                .fetch()
                .flatMap(configDeviceEntity -> {
                    TestEnergyDetailEntity energyDetailEntity = new TestEnergyDetailEntity();
                    energyDetailEntity.setTestRecordId(entity.getId());
                    energyDetailEntity.setDeviceId(configDeviceEntity.getDeviceId());
                    energyDetailEntity.setDeviceName(configDeviceEntity.getDeviceName());
                    return testEnergyDetailService.insert(energyDetailEntity);
                })
            ).then();
    }

    //统计能耗和费用方法
    public Mono<Void> energyCostStat(List<TestRecordEntity> testRecordList) {
        for (TestRecordEntity entity: testRecordList) {
            long time = System.currentTimeMillis();
            if(time - entity.getTestEndTime() >= 86400000){
                return service.createUpdate()
                    .set(TestRecordEntity::getItemStatus,2)
                    .where(TestRecordEntity::getId,entity.getId())
                    .not(TestRecordEntity::getItemStatus,2)
                    .execute()
                    .flatMap(e->{
                        Disposable disposable = disposableHashMap.get(entity.getId());
                        if(Objects.nonNull(disposable)){
                            disposable.dispose();
                            disposableHashMap.remove(entity.getId());
                        }
                        return Mono.empty();
                    });
            }
        }
        AtomicReference<BigDecimal> testExpenses = new AtomicReference<>(new BigDecimal("0"));
        return Flux
            .fromIterable(testRecordList)
            .flatMap(recordEntity ->
                         testEnergyDetailService
                             .createQuery()
                             //根据试验记录的id查询试验记录的能耗详情
                             .where(TestEnergyDetailEntity::getTestRecordId, recordEntity.getId())
                             .fetch()
                             .flatMap(testEnergyDetailEntity -> {
                                 //定义水、电、气用量的初始值
                                 AtomicReference<BigDecimal> water = new AtomicReference<>(new BigDecimal("0"));
                                 AtomicReference<BigDecimal> electricity = new AtomicReference<>(new BigDecimal("0"));
                                 AtomicReference<BigDecimal> gas = new AtomicReference<>(new BigDecimal("0"));
                                 AtomicReference<BigDecimal> waterPrice = new AtomicReference<>(new BigDecimal("0"));
                                 AtomicReference<BigDecimal> electricityPrice = new AtomicReference<>(new BigDecimal("0"));
                                 AtomicReference<BigDecimal> gasPrice= new AtomicReference<>(new BigDecimal("0"));

                                 return Flux
                                     .concat(
                                         queryHelper
                                             .select("SELECT \n" +
                                                         "* \n" +
                                                         "FROM sems_gas_consume\n" +
                                                         "WHERE device_id = ? \n" +
                                                         "and gather_time > ? \n" +
                                                         "and gather_time/1000 <= ?", GasConsumeEntity::new
                                                 , testEnergyDetailEntity.getDeviceId(), recordEntity.getTestStartTime(), recordEntity
                                                         .getTestEndTime())
                                             .fetch()
                                         , queryHelper
                                             .select("SELECT \n" +
                                                         "* \n" +
                                                         "FROM sems_water_consume\n" +
                                                         "WHERE device_id = ? \n" +
                                                         "and gather_time > ? \n" +
                                                         "and gather_time/1000 <= ?",WaterConsumeEntity::new
                                                 , testEnergyDetailEntity.getDeviceId(), recordEntity.getTestStartTime(), recordEntity
                                                         .getTestEndTime())
                                             .fetch()
                                         , queryHelper
                                             .select("SELECT \n" +
                                                         "* \n" +
                                                         "FROM sems_electricity_consume\n" +
                                                         "WHERE device_id = ? \n" +
                                                         "and gather_time > ? \n" +
                                                         "and gather_time/1000 <= ?", ElectricityConsumeEntity::new
                                                 , testEnergyDetailEntity.getDeviceId(), recordEntity.getTestStartTime(), recordEntity
                                                         .getTestEndTime())
                                             .fetch()
                                     )
                                     .collectList()
                                     .doOnNext(list -> {
                                         //能耗用量处理
                                         for (Object consume : list) {
                                             if (consume instanceof GasConsumeEntity) {
                                                 GasConsumeEntity gasEntity = (GasConsumeEntity) consume;
                                                 gas.set(gas.get()
                                                            .add(gasEntity.getDifference()));
                                                 gasPrice.set(gasPrice.get()
                                                                      .add(gasEntity.getDifference()
                                                                                    .multiply(gasEntity.getUnitPrice())
                                                                                    .setScale(4, RoundingMode.HALF_UP)));
                                                 testExpenses.set(testExpenses.get()
                                                                              .add(gasEntity.getDifference()
                                                                                            .multiply(gasEntity.getUnitPrice())
                                                                                            .setScale(4, RoundingMode.HALF_UP)));
                                             }
                                             if (consume instanceof WaterConsumeEntity) {
                                                 WaterConsumeEntity waterEntity = (WaterConsumeEntity) consume;
                                                 water.set(water.get()
                                                                .add(waterEntity.getDifference()));
                                                 waterPrice.set(waterPrice.get()
                                                                          .add(waterEntity.getDifference()
                                                                                          .multiply(waterEntity.getUnitPrice())
                                                                                          .setScale(4, RoundingMode.HALF_UP)));
                                                 testExpenses.set(testExpenses.get()
                                                                              .add(waterEntity.getDifference()
                                                                                              .multiply(waterEntity.getUnitPrice())
                                                                                              .setScale(4, RoundingMode.HALF_UP)));
                                             }
                                             if (consume instanceof ElectricityConsumeEntity) {
                                                 ElectricityConsumeEntity electricityEntity = (ElectricityConsumeEntity) consume;
                                                 electricity.set(electricity.get()
                                                                            .add(electricityEntity.getDifference()));
                                                 electricityPrice.set(electricityPrice.get()
                                                                              .add(electricityEntity.getDifference()
                                                                                                    .multiply(electricityEntity
                                                                                                                  .getUnitPrice())
                                                                                                    .setScale(4, RoundingMode.HALF_UP)));
                                                 testExpenses.set(testExpenses.get()
                                                                              .add(electricityEntity.getDifference()
                                                                                                    .multiply(electricityEntity
                                                                                                                  .getUnitPrice())
                                                                                                    .setScale(4, RoundingMode.HALF_UP)));
                                             }
                                         }
                                     }).flatMap(list -> {
                                         //设备为空压冷却塔、锅炉、组合冷却塔、普冷、空调
                                         if( testEnergyDetailEntity.getShareDevice().equals("1") ){
                                             return shareDeviceEnergy(testEnergyDetailEntity)
                                                 .flatMap(shareList ->{
                                                     if(shareList.size() > 0){
                                                         List<JSONObject> crossTimeList = getCrossTime(recordEntity,shareList);
                                                         return Flux.fromIterable(crossTimeList)
                                                                    .flatMap(crossTime -> queryHelper
                                                                        .select("SELECT \n" +
                                                                                    "* \n" +
                                                                                    "FROM sems_electricity_consume\n" +
                                                                                    "WHERE device_id = ? \n" +
                                                                                    "and FROM_UNIXTIME(FLOOR(gather_time/1000)) > FROM_UNIXTIME(FLOOR(?/1000)) \n" +
                                                                                    "and FROM_UNIXTIME(FLOOR(gather_time/1000)) <= FROM_UNIXTIME(FLOOR(?/1000))", ElectricityConsumeEntity::new,
                                                                                testEnergyDetailEntity.getDeviceId(),
                                                                                crossTime.getLong("startTime"),
                                                                                crossTime.getLong("endTime"))
                                                                        .fetch()
                                                                        .collectList()
                                                                        .doOnNext(energyList ->{
                                                                            for (ElectricityConsumeEntity consume : energyList) {
                                                                                electricity.set(electricity.get()
                                                                                                           .subtract(consume.getDifference()
                                                                                                                            .divide(BigDecimal.valueOf(2),2,RoundingMode.HALF_UP)));
                                                                                electricityPrice.set(electricityPrice.get()
                                                                                                                     .subtract(consume.getDifference()
                                                                                                                                      .multiply(consume.getUnitPrice())
                                                                                                                                      .divide(BigDecimal.valueOf(2),2,RoundingMode.HALF_UP)
                                                                                                                                      .setScale(4, RoundingMode.HALF_UP)));
                                                                            }
                                                                        })).collectList();
                                                     }
                                                     return Mono.just(list);
                                                 });
                                         }
                                         return Mono.just(list);
                                     })
                                     .flatMap(e ->testEnergyDetailService
                                                  .createUpdate()
                                                  .set(TestEnergyDetailEntity::getGas, gas.get())
                                                  .set(TestEnergyDetailEntity::getWater, water.get())
                                                  .set(TestEnergyDetailEntity::getElectricity, electricity.get())
                                                  .set(TestEnergyDetailEntity::getGasPrice,gasPrice.get())
                                                  .set(TestEnergyDetailEntity::getWaterPrice,waterPrice.get())
                                                  .set(TestEnergyDetailEntity::getElectricityPrice,electricityPrice.get())
                                                  .where(TestEnergyDetailEntity::getId, testEnergyDetailEntity.getId())
                                                  .execute());
                             })
                             .flatMap(e -> service.createUpdate()
                                                  .set(TestRecordEntity::getTestExpenses, testExpenses.get())
                                                  .where(TestRecordEntity::getId, recordEntity.getId())
                                                  .execute()
                                                  .flatMap(record->{
                                                      //试验告警
                                                      String waterAlarm = "Rule:" + recordEntity.getConfigId() + "1" + "water";
                                                      String gasAlarm = "Rule:" + recordEntity.getConfigId() + "1" + "gas";
                                                      String electricityAlarm = "Rule:" + recordEntity.getConfigId() + "1" + "electricity";
                                                      return redis
                                                          .hasKey(waterAlarm)
                                                          .flatMap(hasRedis -> {
                                                              if (hasRedis) {
                                                                  return redis
                                                                      .opsForValue()
                                                                      .get(waterAlarm)
                                                                      .flatMap(alarmRule -> {
                                                                          AlarmRuleEntity alarmRuleEntity = (AlarmRuleEntity) alarmRule;
                                                                          if(alarmRuleEntity.getRuleStatus().equals("0")){
                                                                              return Mono.empty();
                                                                          }
                                                                          return queryHelper
                                                                              .select("SELECT\n" +
                                                                                          "sum( ted.water ) AS difference \n" +
                                                                                          "FROM\n" +
                                                                                          "sems_test_energy_detail ted \n" +
                                                                                          "WHERE ted.test_record_id = ", EnergyDayRes::new)
                                                                              .fetch()
                                                                              .mapNotNull(EnergyDayRes::getDifference)
                                                                              .reduce(BigDecimal.ZERO,BigDecimal::add)
                                                                              .flatMap(difference -> alarmRecordsEventHandler
                                                                                  .testRuleCheck(alarmRuleEntity,difference));


                                                                      });
                                                              } else {
                                                                  return Mono.empty();
                                                              }
                                                          })
                                                          .then(
                                                              redis
                                                                  .hasKey(gasAlarm)
                                                                  .flatMap(hasRedis -> {
                                                                      if (hasRedis) {
                                                                          return redis
                                                                              .opsForValue()
                                                                              .get(gasAlarm)
                                                                              .flatMap(alarmRule -> {
                                                                                  AlarmRuleEntity alarmRuleEntity = (AlarmRuleEntity) alarmRule;
                                                                                  if(alarmRuleEntity.getRuleStatus().equals("0")){
                                                                                      return Mono.empty();
                                                                                  }
                                                                                  return queryHelper
                                                                                      .select("SELECT\n" +
                                                                                                  "sum( ted.gas ) AS difference \n" +
                                                                                                  "FROM\n" +
                                                                                                  "sems_test_energy_detail ted \n" +
                                                                                                  "WHERE ted.test_record_id = ", EnergyDayRes::new)
                                                                                      .fetch()
                                                                                      .mapNotNull(EnergyDayRes::getDifference)
                                                                                      .reduce(BigDecimal.ZERO,BigDecimal::add)
                                                                                      .flatMap(difference -> alarmRecordsEventHandler
                                                                                          .testRuleCheck(alarmRuleEntity,difference));
                                                                              });
                                                                      } else {
                                                                          return Mono.empty();
                                                                      }
                                                                  })
                                                          ).then(
                                                              redis
                                                                  .hasKey(electricityAlarm)
                                                                  .flatMap(hasRedis -> {
                                                                      if (hasRedis) {
                                                                          return redis
                                                                              .opsForValue()
                                                                              .get(electricityAlarm)
                                                                              .flatMap(alarmRule -> {
                                                                                  AlarmRuleEntity alarmRuleEntity = (AlarmRuleEntity) alarmRule;
                                                                                  if(alarmRuleEntity.getRuleStatus().equals("0")){
                                                                                      return Mono.empty();
                                                                                  }
                                                                                  return queryHelper
                                                                                      .select("SELECT\n" +
                                                                                                  "sum( ted.electricity ) AS difference \n" +
                                                                                                  "FROM\n" +
                                                                                                  "sems_test_energy_detail ted \n" +
                                                                                                  "WHERE ted.test_record_id = ", EnergyDayRes::new)
                                                                                      .fetch()
                                                                                      .mapNotNull(EnergyDayRes::getDifference)
                                                                                      .reduce(BigDecimal.ZERO,BigDecimal::add)
                                                                                      .flatMap(difference -> alarmRecordsEventHandler
                                                                                          .testRuleCheck(alarmRuleEntity,difference));
                                                                              });
                                                                      } else {
                                                                          return Mono.empty();
                                                                      }
                                                                  })
                                                          );
                                                  }))
            )
            .then();
    }


    //查看是否有试验的实验时间交叉
    private Mono<List<TestRecordEntity>> shareDeviceEnergy(TestEnergyDetailEntity detailEntity){
        return service.createQuery()
                      .where(TestRecordEntity::getId,detailEntity.getTestRecordId())
                      .fetchOne()
                      .flatMap(record ->{
                          if(Objects.isNull(record.getTestEndTime())){
                              record.setTestEndTime(System.currentTimeMillis());
                          }
                          return queryHelper
                              .select("SELECT \n" +
                                          "*\n" +
                                          "FROM \n" +
                                          "sems_test_record\n" +
                                          "WHERE ( ? <= test_end_time AND ? >= test_start_time)\n" +
                                          "OR ( ? >= test_start_time AND ? <= test_end_time)\n" +
                                          "AND id != ?",TestRecordEntity::new,
                                      record.getTestStartTime(),record.getTestEndTime(),
                                      record.getTestStartTime(),record.getTestEndTime(),
                                      record.getId())
                              .fetch()
                              .collectList()
                              .flatMap(Mono::just);
                      });
    }

    private List<JSONObject> getCrossTime(TestRecordEntity recordEntity,List<TestRecordEntity> recordList){
        List<JSONObject> timeList= new ArrayList<>();

        TestRecordEntity current = new TestRecordEntity();
        current.setTestStartTime(recordEntity.getTestStartTime());
        current.setTestEndTime(recordEntity.getTestEndTime());
        for (TestRecordEntity entity:recordList) {
            if(Objects.isNull(entity.getTestEndTime())){
                entity.setTestEndTime(System.currentTimeMillis());
            }

            Long recordStartTime = current.getTestStartTime();
            Long recordEndTime = current.getTestEndTime();

            Long nextStart = entity.getTestStartTime();
            Long nextEnd = entity.getTestEndTime();

            Long start = Math.max(recordStartTime, nextStart);
            Long end = Math.min(recordEndTime, nextEnd);
            if (end >= start) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("startTime",start);
                jsonObject.put("endTime",end);
                timeList.add(jsonObject);
            }
        }
        return timeList;
    }

}
