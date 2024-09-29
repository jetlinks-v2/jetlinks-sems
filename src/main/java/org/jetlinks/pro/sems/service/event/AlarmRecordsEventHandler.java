package org.jetlinks.pro.sems.service.event;

import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.events.EntityBeforeCreateEvent;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.sems.constant.AlarmRuleConstant;
import org.jetlinks.pro.sems.entity.AlarmRecordsEntity;
import org.jetlinks.pro.sems.entity.AlarmRuleEntity;
import org.jetlinks.pro.sems.entity.req.ThreePresentReq;
import org.jetlinks.pro.sems.entity.res.EnergyDayRes;
import org.jetlinks.pro.sems.service.AlarmRecordsService;
import org.jetlinks.pro.sems.service.AlarmRuleService;
import org.jetlinks.pro.sems.service.UserAndVxUserIdService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class AlarmRecordsEventHandler {

    private final AlarmRecordsService alarmRecordsService;

    private final AlarmRuleService alarmRuleService;

    private final QueryHelper queryHelper;

//    private final abutmentService mentService;

    private final UserAndVxUserIdService userAndVxUserIdService;

//    private final UserDetailService userDetailService;


    @EventListener
    public void handleCreatedEvent(EntityBeforeCreateEvent<AlarmRecordsEntity> event){

        event.async(this.sendBeforeCreatedNotify(event.getEntity()));

    }

    public Mono<Void> sendBeforeCreatedNotify(List<AlarmRecordsEntity> alarmRecordsList){

        return Flux
            .fromIterable(alarmRecordsList)
            .flatMap(alarmRecords -> alarmRuleService
                .findById(alarmRecords.getRuleId())
                .flatMap(ruleEntity ->{
                        return queryHelper
                            .select("SELECT\n" +
                                        "*\n" +
                                        "FROM\n" +
                                        "sems_alarm_records\n" +
                                        "WHERE \n" +
                                        "DATE(FROM_UNIXTIME(SUBSTR( create_time, 1, 10 ))) = DATE(NOW())\n" +
                                        "AND rule_id = '"+ ruleEntity.getId() +"'\n" +
                                        "ORDER BY create_time DESC",AlarmRecordsEntity::new)
                            .fetch()
                            .collectList()
                            .flatMap(list ->{
                                if(ruleEntity.getAlarmType().equals("0")
                                    && ruleEntity.getRuleType().equals("1")
                                    && ruleEntity.getAntiShakeStatus().equals("1")) {
                                    if (list.size() < 1) {
                                        return Mono.empty();
                                    } else if (alarmRecords.getCreateTime() - list.get(0).getCreateTime()
                                        >= (ruleEntity.getAntiShakeTime() * 1000)) {
                                        return Mono.empty();
                                    } else {
                                        return Mono.error(new RuntimeException(ruleEntity.getAntiShakeTime()
                                                                                   + "秒内功率告警只生成一条记录"));
                                    }
                                } else if(ruleEntity.getAlarmType().equals("0")
                                    && ruleEntity.getRuleType().equals("1")
                                    && ruleEntity.getAntiShakeStatus().equals("0")) {
                                    return Mono.empty();
                                } else {
                                    if(list.size() < 1){
                                        return Mono.empty();
                                    } else if(list.size() < 3 && alarmRecords.getCreateTime() - list.get(0).getCreateTime() >= 3600000){
                                        return Mono.empty();
                                    } else {
                                        return Mono.error(new RuntimeException("一小时内同一个告警规则只生成一条记录"));
                                    }
                                }
                            });
                })
            )
            .then();
    }

    //功率规则告警
    public Mono<Void> powerRuleCheck(AlarmRuleEntity alarmRuleEntity, JSONObject consumeEntity){
        //初始化告警记录
        AlarmRecordsEntity recordsEntity = initializationAlarmRecord(alarmRuleEntity,consumeEntity);
        recordsEntity.setRuleType(alarmRuleEntity.getRuleType());

        if(consumeEntity.getBigDecimal("power").compareTo(alarmRuleEntity.getThreshold()) > 0){
            recordsEntity.setCurrentValue(consumeEntity.getBigDecimal("power"));
            String content = recordsEntity.getName() + "的功率超值设定为" + alarmRuleEntity.getThreshold() + "千瓦," +
                "当前功率为" + recordsEntity.getCurrentValue()+"千瓦";
            recordsEntity.setAlarmContent(content);
            return generateAlarmCode(recordsEntity)
                .flatMap(alarmRecordsService::insert).then();
        }
        return Mono.empty();
    }

    //能耗规则告警
    public Mono<Void> energyValueRuleCheck(AlarmRuleEntity alarmRuleEntity, JSONObject consumeEntity){
        //初始化告警记录
        AlarmRecordsEntity recordsEntity = initializationAlarmRecord(alarmRuleEntity,consumeEntity);
        recordsEntity.setRuleType(alarmRuleEntity.getRuleType());

        LocalDate gatherTime = Instant.ofEpochMilli(recordsEntity.getAlarmTime())
                                          .atZone(ZoneId.systemDefault())
                                          .toLocalDate();
        LocalTime time = Instant.ofEpochMilli(recordsEntity.getAlarmTime())
                                      .atZone(ZoneId.systemDefault())
                                      .toLocalTime();
        long starTime = 0;
        long endTime = 0;
        if(alarmRuleEntity.getSettingStatus().equals("0")){
            starTime = LocalDateTime.of(gatherTime.minusDays(30), LocalTime.MIN)
                                    .toInstant(ZoneOffset.of("+8")).toEpochMilli();
            endTime = LocalDateTime.of(gatherTime.minusDays(1),time)
                                   .toInstant(ZoneOffset.of("+8")).toEpochMilli();

            return queryHelper
                .select("SELECT \n" +
                            "max(a.difference) as difference \n" +
                            "FROM \n" +
                            "(SELECT \n" +
                            "sum(difference) as difference \n" +
                            "FROM\n" +
                            "sems_"+ alarmRuleEntity.getEnergyType().getValue()+"_consume\n" +
                            "WHERE gather_time BETWEEN "+ starTime +" and "+ endTime +"\n" +
                            "AND device_id = ? \n"+
                            "GROUP BY DATE(FROM_UNIXTIME(SUBSTR( gather_time, 1, 10 ))) ) a",EnergyDayRes::new
                    ,recordsEntity.getAlarmTypeId())
                .fetch()
                .switchIfEmpty(Flux.just(new EnergyDayRes()))
                .flatMap(e -> {
                    //判断近30天是否有数据，无数据取历史数据中最大的一天的数据
                    if(Objects.isNull(e.getDifference())){
                        return queryHelper
                            .select("SELECT \n" +
                                        "max(a.difference) as difference \n" +
                                        "FROM \n" +
                                        "(SELECT \n" +
                                        "sum(difference) as difference \n" +
                                        "FROM\n" +
                                        "sems_"+ alarmRuleEntity.getEnergyType().getValue()+"_consume\n" +
                                        "WHERE device_id = ? \n"+
                                        "GROUP BY DATE(FROM_UNIXTIME(SUBSTR( gather_time, 1, 10 ))) ) a",EnergyDayRes::new
                                ,recordsEntity.getAlarmTypeId())
                            .fetch()
                            .flatMap(res ->{
                                BigDecimal threshold = res.getDifference()
                                                        .add(res.getDifference()
                                                              .multiply(alarmRuleEntity.getPercentage()));
                                return queryHelper
                                    .select("SELECT\n" +
                                                "SUM( difference ) AS difference \n" +
                                                "FROM\n" +
                                                "sems_"+ recordsEntity.getEnergyType().getValue() +"_consume \n" +
                                                "WHERE device_id = ? \n"+
                                                "AND DATE(FROM_UNIXTIME(SUBSTR( gather_time, 1, 10 ))) = DATE(NOW())",EnergyDayRes::new
                                        ,recordsEntity.getAlarmTypeId())
                                    .fetch()
                                    .mapNotNull(EnergyDayRes::getDifference)
                                    .reduce(BigDecimal.ZERO,BigDecimal::add)
                                    .flatMap(currentValue ->{
                                        if(currentValue.compareTo(threshold) > 0 ){
                                            recordsEntity.setCurrentValue(currentValue.setScale(2,3));
                                            recordsEntity.setAlarmContent(createAlarmContent(recordsEntity));
                                            return generateAlarmCode(recordsEntity)
                                                .flatMap(alarmRecordsService::insert);
                                        }
                                        return Mono.empty();
                                    });
                            });
                    }

                    //获取阈值
                    BigDecimal threshold = e.getDifference()
                                            .add(e.getDifference()
                                                  .multiply(alarmRuleEntity.getPercentage()));
                    return queryHelper
                        .select("SELECT\n" +
                                    "SUM( difference ) AS difference \n" +
                                    "FROM\n" +
                                    "sems_"+ recordsEntity.getEnergyType().getValue() +"_consume \n" +
                                    "WHERE device_id = ? \n"+
                                    "AND DATE(FROM_UNIXTIME(SUBSTR( gather_time, 1, 10 ))) = DATE(NOW())",EnergyDayRes::new,
                                recordsEntity.getAlarmTypeId())
                        .fetch()
                        .mapNotNull(EnergyDayRes::getDifference)
                        .reduce(BigDecimal.ZERO,BigDecimal::add)
                        .flatMap(currentValue ->{
                            if(currentValue.compareTo(threshold) > 0 ){
                                recordsEntity.setThreshold(threshold);
                                recordsEntity.setCurrentValue(currentValue.setScale(2,3));
                                recordsEntity.setAlarmContent(createAlarmContent(recordsEntity));
                                return generateAlarmCode(recordsEntity)
                                    .flatMap(alarmRecordsService::insert);
                            }
                            return Mono.empty();
                        });
                })
                .then();
        }

        if(alarmRuleEntity.getSettingStatus().equals("1")){
            return queryHelper
                .select("SELECT\n" +
                            "SUM( difference ) AS difference \n" +
                            "FROM\n" +
                            "sems_"+ recordsEntity.getEnergyType().getValue() +"_consume \n" +
                            "WHERE device_id = ? \n"+
                            "AND DATE(FROM_UNIXTIME(SUBSTR( gather_time, 1, 10 ))) = DATE(NOW())",EnergyDayRes::new
                    ,recordsEntity.getAlarmTypeId())
                .fetch()
                .mapNotNull(EnergyDayRes::getDifference)
                .reduce(BigDecimal.ZERO,BigDecimal::add)
                .flatMap(currentValue ->{
                    if(currentValue.compareTo(alarmRuleEntity.getThreshold()) > 0 ){
                        recordsEntity.setCurrentValue(currentValue.setScale(2,3));
                        recordsEntity.setAlarmContent(createAlarmContent(recordsEntity));
                        return generateAlarmCode(recordsEntity)
                            .flatMap(alarmRecordsService::insert);
                    }
                    return Mono.empty();
                })
                .then();
        }
        return Mono.empty();

    }

    //场地规则告警
    public Mono<Void> placeRuleCheck(AlarmRuleEntity alarmRuleEntity, JSONObject consumeEntity){
        //初始化告警记录
        AlarmRecordsEntity recordsEntity = initializationAlarmRecord(alarmRuleEntity,consumeEntity);
        LocalDate gatherTime = Instant.ofEpochMilli(recordsEntity.getAlarmTime())
                                      .atZone(ZoneId.systemDefault())
                                      .toLocalDate();
        LocalTime time = Instant.ofEpochMilli(recordsEntity.getAlarmTime())
                                .atZone(ZoneId.systemDefault())
                                .toLocalTime();
        long starTime = 0;
        long endTime = 0;
        if(alarmRuleEntity.getSettingStatus().equals("0")){
            starTime = LocalDateTime.of(gatherTime.minusDays(30), LocalTime.MIN)
                                    .toInstant(ZoneOffset.of("+8")).toEpochMilli();
            endTime = LocalDateTime.of(gatherTime.minusDays(1),time)
                                   .toInstant(ZoneOffset.of("+8")).toEpochMilli();
            //获取场地时间范围内的能耗最大值
            return queryHelper
                .select("SELECT \n" +
                            "max(a.difference) as difference \n" +
                            "FROM \n" +
                            "(SELECT \n" +
                            "sum(cs.difference) as difference \n" +
                            "FROM sems_device_info di \n" +
                            "LEFT JOIN sems_"+ alarmRuleEntity.getEnergyType().getValue() +"_consume cs on di.device_id = cs.device_id\n" +
                            "WHERE cs.gather_time BETWEEN "+ starTime +" and "+ endTime +"\n" +
                            "AND di.place_id = '"+ alarmRuleEntity.getAlarmTypeId() +"' AND di.parent_id = 0\n" +
                            "GROUP BY DATE(FROM_UNIXTIME(SUBSTR( cs.gather_time, 1, 10 ))) ) a",EnergyDayRes::new)
                .fetch()
                .switchIfEmpty(Flux.just(new EnergyDayRes()))
                .flatMap(e -> {
                    //判断近30天是否有数据，无数据取历史数据中最大的一天的数据
                    if(Objects.isNull(e.getDifference())){
                        return queryHelper
                            .select("SELECT \n" +
                                        "max(a.difference) as difference \n" +
                                        "FROM \n" +
                                        "(SELECT \n" +
                                        "sum(cs.difference) as difference \n" +
                                        "FROM sems_device_info di \n" +
                                        "LEFT JOIN sems_"+ alarmRuleEntity.getEnergyType().getValue() +"_consume cs on di.device_id = cs.device_id\n" +
                                        "WHERE di.place_id = "+ alarmRuleEntity.getAlarmTypeId() +" AND di.parent_id = 0\n" +
                                        "GROUP BY DATE(FROM_UNIXTIME(SUBSTR( cs.gather_time, 1, 10 ))) ) a",EnergyDayRes::new)
                            .fetch()
                            .flatMap(res ->{
                                //获取阈值
                                BigDecimal threshold = res.getDifference()
                                                          .add(res.getDifference()
                                                                  .multiply(alarmRuleEntity.getPercentage()));
                                return queryHelper
                                    .select("SELECT \n" +
                                                "sum(cs.difference) as difference \n" +
                                                "FROM sems_device_info di \n" +
                                                "LEFT JOIN sems_"+ alarmRuleEntity.getEnergyType().getValue() +"_consume cs \n" +
                                                "on di.device_id = cs.device_id\n" +
                                                "WHERE di.place_id = "+ alarmRuleEntity.getAlarmTypeId() +" AND di.parent_id = 0\n" +
                                                "AND DATE(FROM_UNIXTIME(SUBSTR( cs.gather_time, 1, 10 ))) = DATE(NOW())",EnergyDayRes::new)
                                    .fetch()
                                    .mapNotNull(EnergyDayRes::getDifference)
                                    .reduce(BigDecimal.ZERO,BigDecimal::add)
                                    .flatMap(currentValue ->{
                                        if(currentValue.compareTo(threshold) > 0 ){
                                            recordsEntity.setCurrentValue(currentValue.setScale(2,3));
                                            recordsEntity.setThreshold(threshold);
                                            recordsEntity.setAlarmContent(createAlarmContent(recordsEntity));
                                            return generateAlarmCode(recordsEntity)
                                                .flatMap(alarmRecordsService::insert);
                                        }
                                        return Mono.empty();
                                    })
                                    .then();
                            });
                    }
                    //获取阈值
                    BigDecimal threshold = e.getDifference()
                                              .add(e.getDifference()
                                                      .multiply(alarmRuleEntity.getPercentage()));
                    return queryHelper
                        .select("SELECT \n" +
                                    "sum(cs.difference) as difference \n" +
                                    "FROM sems_device_info di \n" +
                                    "LEFT JOIN sems_"+ alarmRuleEntity.getEnergyType().getValue() +"_consume cs \n" +
                                    "on di.device_id = cs.device_id\n" +
                                    "WHERE di.place_id = '"+ alarmRuleEntity.getAlarmTypeId() +"' AND di.parent_id = 0\n" +
                                    "AND DATE(FROM_UNIXTIME(SUBSTR( cs.gather_time, 1, 10 ))) = DATE(NOW())",EnergyDayRes::new)
                        .fetch()
                        .mapNotNull(EnergyDayRes::getDifference)
                        .reduce(BigDecimal.ZERO,BigDecimal::add)
                        .flatMap(currentValue ->{
                            if(currentValue.compareTo(threshold) > 0 ){
                                recordsEntity.setCurrentValue(currentValue.setScale(2,3));
                                recordsEntity.setThreshold(threshold);
                                recordsEntity.setAlarmContent(createAlarmContent(recordsEntity));
                                return generateAlarmCode(recordsEntity)
                                    .flatMap(alarmRecordsService::insert);
                            }
                            return Mono.empty();
                        })
                        .then();
                })
                .then();
        }

        if(alarmRuleEntity.getSettingStatus().equals("1")){
            return queryHelper
                .select("SELECT \n" +
                            "sum(cs.difference) as difference \n" +
                            "FROM sems_device_info di \n" +
                            "LEFT JOIN sems_"+ alarmRuleEntity.getEnergyType().getValue() +"_consume cs \n" +
                            "on di.device_id = cs.device_id\n" +
                            "WHERE di.place_id = '"+ alarmRuleEntity.getAlarmTypeId() +"' AND di.parent_id = 0\n" +
                            "AND DATE(FROM_UNIXTIME(SUBSTR( cs.gather_time, 1, 10 ))) = DATE(NOW())",EnergyDayRes::new)
                .fetch()
                .mapNotNull(EnergyDayRes::getDifference)
                .reduce(BigDecimal.ZERO,BigDecimal::add)
                .flatMap(currentValue ->{
                    if(currentValue.compareTo(alarmRuleEntity.getThreshold()) > 0 ){
                        recordsEntity.setCurrentValue(currentValue.setScale(2,3));
                        recordsEntity.setAlarmContent(createAlarmContent(recordsEntity));
                        return generateAlarmCode(recordsEntity)
                            .flatMap(alarmRecordsService::insert);
                    }
                    return Mono.empty();
                })
                .then();
        }

        return Mono.empty();
    }

    //试验规则告警
    public Mono<Void> testRuleCheck(AlarmRuleEntity alarmRuleEntity, BigDecimal energyValue){
        //初始化告警记录
        AlarmRecordsEntity recordsEntity = new AlarmRecordsEntity();
        recordsEntity.setRuleId(alarmRuleEntity.getId());
        recordsEntity.setRuleType(alarmRuleEntity.getRuleType());
        recordsEntity.setRuleName(alarmRuleEntity.getRuleName());
        recordsEntity.setEnergyType(alarmRuleEntity.getEnergyType());
        recordsEntity.setAlarmType(alarmRuleEntity.getAlarmType());
        recordsEntity.setAlarmTypeId(alarmRuleEntity.getAlarmTypeId());
        recordsEntity.setName(alarmRuleEntity.getName());
        recordsEntity.setSettingStatus(alarmRuleEntity.getSettingStatus());
        recordsEntity.setThreshold(alarmRuleEntity.getThreshold());
        recordsEntity.setDescr(alarmRuleEntity.getDescr());
        recordsEntity.setAlarmTime(System.currentTimeMillis());


        LocalTime time = Instant.ofEpochMilli(recordsEntity.getAlarmTime())
                                .atZone(ZoneId.systemDefault())
                                .toLocalTime();
        LocalDate gatherTime = Instant.ofEpochMilli(recordsEntity.getAlarmTime())
                                      .atZone(ZoneId.systemDefault())
                                      .toLocalDate();
        long starTime = 0;
        long endTime = 0;
        if(alarmRuleEntity.getSettingStatus().equals("0")){
            starTime = LocalDateTime.of(gatherTime.minusDays(30), LocalTime.MIN)
                                    .toInstant(ZoneOffset.of("+8")).toEpochMilli();
            endTime = LocalDateTime.of(gatherTime.minusDays(1),time)
                                   .toInstant(ZoneOffset.of("+8")).toEpochMilli();

            return queryHelper
                .select("SELECT\n" +
                            "max(a.difference) as difference\n" +
                            "FROM\n" +
                            "(SELECT\n" +
                            "tr.id,\n" +
                            "sum( ted."+ alarmRuleEntity.getEnergyType().getValue() +" ) AS difference \n" +
                            "FROM\n" +
                            "sems_test_record tr\n" +
                            "LEFT JOIN sems_test_energy_detail ted ON tr.id = ted.test_record_id\n" +
                            "WHERE ted.modify_time BETWEEN "+ starTime +" and "+ endTime +"\n" +
                            "AND tr.config_id = "+ alarmRuleEntity.getAlarmTypeId() +"\n" +
                            "GROUP BY tr.id ) a",EnergyDayRes::new)
                .fetch()
                .switchIfEmpty(Flux.just(new EnergyDayRes()))
                .flatMap(e -> {
                    //判断近30天是否有数据，无数据取历史数据中最大的一天的数据
                    if(Objects.isNull(e.getDifference())){
                        return queryHelper
                            .select("SELECT\n" +
                                        "max(a.difference) as difference\n" +
                                        "FROM\n" +
                                        "(SELECT\n" +
                                        "tr.id,\n" +
                                        "sum( ted."+ alarmRuleEntity.getEnergyType().getValue() +" ) AS difference \n" +
                                        "FROM\n" +
                                        "sems_test_record tr\n" +
                                        "LEFT JOIN sems_test_energy_detail ted ON tr.id = ted.test_record_id\n" +
                                        "WHERE tr.config_id = "+ alarmRuleEntity.getAlarmTypeId() +"\n" +
                                        "GROUP BY tr.id ) a",EnergyDayRes::new)
                            .fetch()
                            .flatMap(res ->{
                                BigDecimal threshold = res.getDifference()
                                                          .add(res.getDifference()
                                                                  .multiply(alarmRuleEntity.getPercentage()));
                                if(energyValue.compareTo(threshold) > 0 ){
                                    recordsEntity.setCurrentValue(energyValue.setScale(2,3));
                                    recordsEntity.setAlarmContent(createAlarmContent(recordsEntity));
                                    return generateAlarmCode(recordsEntity)
                                        .flatMap(alarmRecordsService::insert);
                                }
                                return Mono.empty();
                            });
                    }

                    //获取阈值
                    BigDecimal threshold = e.getDifference()
                                            .add(e.getDifference()
                                                  .multiply(alarmRuleEntity.getPercentage()));

                    if(energyValue.compareTo(threshold) > 0 ){
                        recordsEntity.setThreshold(threshold);
                        recordsEntity.setCurrentValue(energyValue.setScale(2,3));
                        recordsEntity.setAlarmContent(createAlarmContent(recordsEntity));
                        return generateAlarmCode(recordsEntity)
                            .flatMap(alarmRecordsService::insert);
                    }
                    return Mono.empty();
                })
                .then();
        }

        if(alarmRuleEntity.getSettingStatus().equals("1")){
            if(energyValue.compareTo(alarmRuleEntity.getThreshold()) > 0 ){
                recordsEntity.setCurrentValue(energyValue.setScale(2,3));
                recordsEntity.setAlarmContent(createAlarmContent(recordsEntity));
                return generateAlarmCode(recordsEntity)
                    .flatMap(alarmRecordsService::insert).then();
            }
            return Mono.empty();
        }
        return Mono.empty();

    }

    //生成告警记录实体
    public AlarmRecordsEntity initializationAlarmRecord(AlarmRuleEntity alarmRuleEntity, JSONObject consumeEntity){
        AlarmRecordsEntity recordsEntity = new AlarmRecordsEntity();
        recordsEntity.setRuleId(alarmRuleEntity.getId());
        recordsEntity.setRuleName(alarmRuleEntity.getRuleName());
        recordsEntity.setEnergyType(alarmRuleEntity.getEnergyType());
        recordsEntity.setAlarmType(alarmRuleEntity.getAlarmType());
        recordsEntity.setAlarmTypeId(alarmRuleEntity.getAlarmTypeId());
        recordsEntity.setName(alarmRuleEntity.getName());
        recordsEntity.setSettingStatus(alarmRuleEntity.getSettingStatus());
        recordsEntity.setThreshold(alarmRuleEntity.getThreshold());
        recordsEntity.setDescr(alarmRuleEntity.getDescr());
        recordsEntity.setAlarmTime(consumeEntity.getLong("gatherTime"));

        return recordsEntity;
    }

    //生成告警内容
    public String createAlarmContent(AlarmRecordsEntity recordsEntity){
        String typeString = "";
        String valueString = "";
        String settingString = "";
        if(recordsEntity.getEnergyType().getValue().equals("water")){
            typeString = "耗水量";
            valueString = "吨";
        }
        if(recordsEntity.getEnergyType().getValue().equals("electricity")){
            typeString = "耗电量";
            valueString = "千瓦时";
        }
        if(recordsEntity.getEnergyType().getValue().equals("gas")){
            typeString = "燃气量";
            valueString = "立方米";
        }
        if(recordsEntity.getSettingStatus().equals("0")){
            settingString = "历史能耗最高值上浮"+recordsEntity.getThreshold()+"%";
        } else {
            settingString = recordsEntity.getThreshold()+valueString;
        }

        String content = recordsEntity.getName() + "的" + typeString +
            "超标,超值设定为" + settingString +
            ",当前能耗为" + recordsEntity.getCurrentValue() +valueString;

        return content;
    }

    //创建告警编码
    public Mono<AlarmRecordsEntity> generateAlarmCode(AlarmRecordsEntity alarmRecordsEntity){
        LocalDate localDate = LocalDate.now();
        String year = String.valueOf(localDate.getYear()).substring(2);
        String monthValue = localDate.getMonthValue() < 10 ?
            "0"+ localDate.getMonthValue() :String.valueOf(localDate.getMonthValue());
        String days = localDate.getDayOfMonth() < 10 ?
            "0"+ localDate.getDayOfMonth() :String.valueOf(localDate.getDayOfMonth());


        return queryHelper
            .select("SELECT\n" +
                        "*\n" +
                        "FROM\n" +
                        "sems_alarm_records\n" +
                        "WHERE \n" +
                        "DATE(FROM_UNIXTIME(SUBSTR( create_time, 1, 10 ))) = DATE(NOW())\n" +
                        "ORDER BY create_time DESC",AlarmRecordsEntity::new)
            .fetch()
            .collectList()
            .flatMap(list ->{
                String alarmCode = "GJ" + AlarmRuleConstant.SYS_NAME + year + monthValue + days;
                if(list.size() < 9){
                    alarmCode = alarmCode + "000" + (list.size()+1);
                }
                else if(list.size() < 99){
                    alarmCode = alarmCode + "00" + (list.size()+1);
                }
                else if(list.size() < 999){
                    alarmCode = alarmCode + "0" + (list.size()+1);
                }
                alarmRecordsEntity.setAlarmCode(alarmCode);

                return Mono.just(alarmRecordsEntity);
            });
    }


//    @EventListener
//    public void handleCreatedEvent(EntityCreatedEvent<AlarmRecordsEntity> event){
//
//
//        event.async(this.sendCreatedNotify(event.getEntity()));
//
//    }

//    public Flux<Object> sendCreatedNotify(List<AlarmRecordsEntity> alarmRecordsList){
//
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        return Flux
//            .fromIterable(alarmRecordsList)
//            .flatMap(alarmRecords -> {
//                //组装消息
//                MessageContentReq messageContentReq = new MessageContentReq();
//                messageContentReq.setAppSn("EEM");
//                messageContentReq.setMessageType("2");
//                messageContentReq.setOutOrderNo(String.valueOf(new SnowflakeIdWorker().nextId()));
//                messageContentReq.setEquipmentId(alarmRecords.getAlarmTypeId());
//                messageContentReq.setEquipmentName(alarmRecords.getName());
//                messageContentReq.setMsgTitle("设备告警");
//                messageContentReq.setAlarmType(alarmRecords.getAlarmType());
//                messageContentReq.setMsgContent("设备:"+alarmRecords.getAlarmTypeId()+"的"+alarmRecords.getEnergyType().getText()+"能耗在"+
//                                                    dateFormat.format(alarmRecords.getAlarmTime())+"时段超过阀值！");
//                messageContentReq.setNeedOperate(Boolean.FALSE);
//                messageContentReq.setReceiverId("335,311,313");
//
//                ThreePresentReq threePresentReq = createThreePresentReq(alarmRecords);
//
//                return mentService.getUserList()
//                                  .filter(v->v.getUserId().equals("335") || v.getUserId().equals("311") || v.getUserId().equals("313"))
//                                  .collectList()
//                                  .flatMap(po -> {
//                                      List<String> collect = po.stream().map(UserReturnRes::getUserId).collect(Collectors.toList());
//                                      messageContentReq.setReceiverId(String.join(",",collect));
//                                      return mentService.createAlarmWait(threePresentReq)
//                                                        .flatMap(e ->
//                                                            mentService.createAlarmLedger(threePresentReq)
//                                                        );
//                                  })
//                                  .then(mentService.messagePush(messageContentReq))
//                    .thenMany(
//                        this.createMessageUser(alarmRecords)
//                            .flatMap(mentService::senVxMesssage)
//                    );
//            });
//    }

    //创建三呈现实体类
    private ThreePresentReq createThreePresentReq(AlarmRecordsEntity alarmRecord){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String placeName = "";
        switch (alarmRecord.getAlarmType()){
            case "0":
                placeName = "设备";
                break;
            case "1":
                placeName = "试验";
                break;
            case "2":
                placeName = "场所";
                break;
        }
        //组装告警台账
        ThreePresentReq threePresentReq = new ThreePresentReq();
        threePresentReq.setCode(alarmRecord.getAlarmCode());
        threePresentReq.setApp("能源管理 ");
        threePresentReq.setAlarmTime(dateFormat.format(alarmRecord.getAlarmTime()));
        threePresentReq.setAlarmType("能源超标");
        threePresentReq.setAlarmName(alarmRecord.getRuleName());
        threePresentReq.setAlarmContent(placeName+":"+alarmRecord.getName()+"在"+dateFormat.format(alarmRecord.getAlarmTime()) +"时发生了告警!");
        //threePresentReq.setOperation("http://10.130.179.15/#/warningCenter/alarmRecord");
        return threePresentReq;
    }

//    private VXSendReq createMessage(AlarmRecordsEntity alarmRecord,Integer type) {
//        List<VXTemplateRes> template = mentService.getTemplate(alarmRecord, type);
//        if (!template.isEmpty()) {
//            VXTemplateRes vxTemplateRes = template.get(0);
//            VXSendReq vxSendReq = new VXSendReq();
//            vxSendReq.setSendChannel(vxTemplateRes.getSendChannel());
//            vxSendReq.setMsgType(vxTemplateRes.getMsgType());
//            vxSendReq.setMessageTemplateId(vxTemplateRes.getId());
//            vxSendReq.setAppSn("EEM");
//            //组装消息
//            String msgContent = vxTemplateRes.getMsgContent();
//            HashMap<String, String> map = new HashMap<>();
//            String tag = null;
//            String unit = null;
//            if (alarmRecord.getEnergyType().getValue().equals("water")) {
//                tag = "耗水量";
//                unit = "吨";
//            } else if (alarmRecord.getEnergyType().getValue().equals("electricity")) {
//                tag = "耗电量";
//                unit = "千瓦时";
//            } else {
//                tag = "耗气量";
//                unit = "立方米";
//            }
//            if ("0".equals(alarmRecord.getAlarmType())) {
//                //按设备
//                if (!alarmRecord.getRuleType().isEmpty()) {
//                    map.put("device", alarmRecord.getName());
//                    map.put("Alarm value", alarmRecord.getThreshold() + "千瓦");
//                    map.put("current", alarmRecord.getCurrentValue() + "千瓦");
//                } else {
//                    if ("0".equals(alarmRecord.getSettingStatus())) {
//                        //超值限定
//                        map.put("device", alarmRecord.getName());
//                        map.put("value", tag);
//                        map.put("Alarm value", "历史能耗最高值上浮" + alarmRecord.getThreshold() + "%");
//                        map.put("current", alarmRecord.getCurrentValue() + unit);
//                    } else {
//                        map.put("device", alarmRecord.getName());
//                        map.put("value", tag);
//                        map.put("Alarm value", alarmRecord.getThreshold() + unit);
//                        map.put("current", alarmRecord.getCurrentValue() + unit);
//                    }
//                }
//            } else if ("1".equals(alarmRecord.getAlarmType())) {
//                //按试验
//                if ("0".equals(alarmRecord.getSettingStatus())) {
//                    //超值限定
//                    map.put("device", alarmRecord.getName());
//                    map.put("value", tag);
//                    map.put("Alarm value", "历史能耗最高值上浮" + alarmRecord.getThreshold() + "%");
//                    map.put("current", alarmRecord.getCurrentValue() + unit);
//                } else {
//                    map.put("device", alarmRecord.getName());
//                    map.put("value", tag);
//                    map.put("Alarm value", alarmRecord.getThreshold() + unit);
//                    map.put("current", alarmRecord.getCurrentValue() + unit);
//                }
//            } else {
//                if ("0".equals(alarmRecord.getSettingStatus())) {
//                    //超值限定
//                    map.put("device", alarmRecord.getName());
//                    map.put("value", tag);
//                    map.put("Alarm value", "历史能耗最高值上浮" + alarmRecord.getThreshold() + "%");
//                    map.put("current", alarmRecord.getCurrentValue() + unit);
//                } else {
//                    map.put("device", alarmRecord.getName());
//                    map.put("value", tag);
//                    map.put("Alarm value", alarmRecord.getThreshold() + unit);
//                    map.put("current", alarmRecord.getCurrentValue() + unit);
//                }
//            }
//            String content = getNewStr(msgContent, map);
//            vxSendReq.setMsgContent(content);
//            return vxSendReq;
//        }
//        return new VXSendReq();
//    }


//    private Flux<VXSendReq> createMessageUser(AlarmRecordsEntity alarmRecord){
//        return alarmRuleService
//            .findById(alarmRecord.getRuleId())
//            .flatMapMany(value->{
//                //判断是否需要推送
//                if(value.getPushType()==null || "".equals(value.getPushType())){
//                    return Flux.empty();
//                }
//                //接收的人
//                String pushUserId = value.getPushUserId();
//                List<String> userIds = Arrays.asList(pushUserId.split(","));
//
//                //类型
//                String pushType = value.getPushType();
//                List<String> types = Arrays.asList(pushType.split(","));
//
//                return Flux.fromIterable(types)
//                    .flatMap(type->{
////                        if("2".equals(type)) {
////                            //邮件
////                            VXSendReq vxSendReq = this.createMessage(alarmRecord, 40);
////                            if(vxSendReq.getMessageTemplateId()==null){
////                                return Mono.empty();
////                            }
////                            return userDetailService
////                                .createQuery()
////                                .in(UserDetailEntity::getId,userIds)
////                                .fetch()
////                                .map(UserDetailEntity::getEmail)
////                                .collectList()
////                                .flatMap(list->{
////                                    vxSendReq.setReceiver(String.join(",",list));
////                                    return Mono.just(vxSendReq);
////                                });
////                        }else if("0".equals(type)) {
////                            //短信
////                            VXSendReq vxSendReq = this.createMessage(alarmRecord, 30);
////                            if(vxSendReq.getMessageTemplateId()==null){
////                                return Mono.empty();
////                            }
////                            return userDetailService
////                                .createQuery()
////                                .in(UserDetailEntity::getId,userIds)
////                                .fetch()
////                                .map(UserDetailEntity::getTelephone)
////                                .collectList()
////                                .flatMap(list->{
////                                    vxSendReq.setReceiver(String.join(",",list));
////                                    return Mono.just(vxSendReq);
////                                });
////                        }else {
////                            //企业微信
////                            VXSendReq vxSendReq = this.createMessage(alarmRecord, 70);
////                            if(vxSendReq.getMessageTemplateId()==null){
////                                return Mono.empty();
////                            }
////                            return userAndVxUserIdService.query(new QueryParamEntity())
////                                .filter(o->userIds.contains(o.getUserId()))
////                                .map(UserAndVxUserIdEntity::getVxUserId)
////                                .collectList()
////                                .flatMap(list->{
////                                    vxSendReq.setReceiver(String.join("|",list));
////                                    return Mono.just(vxSendReq);
////                                });
////                        }
//                    });
//
//            });
//    }

    private static  String getNewStr(String str, Map<String,String> map) {
        if (str.contains("$")) {
            String patternString = "\\$\\s*\\{\\s*(.+?)\\s*\\}";
            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(str);
            String text = "";
            while (matcher.find()) {
                String keyString = matcher.group(1);
                String value = map.get(keyString);
                text = matcher.replaceFirst(value);
                matcher = pattern.matcher(text);
            }
            return text;
        }
        return str;
    }
//
//    @EventListener
//    public void handleModifyEvent(EntityModifyEvent<AlarmRecordsEntity> event){
//
//        event.async(this.sendUpdateNotify(event.getAfter()));
//
//    }

//    public Mono<Void> sendUpdateNotify(List<AlarmRecordsEntity> alarmRecordList){
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        return Flux.fromIterable(alarmRecordList)
//                   .flatMap(alarmRecords -> {
//                       if(alarmRecords.getStatus().equals("1")){
//                           ThreePresentReq req = createThreePresentReq(alarmRecords);
//                           req.setSolvePeople(alarmRecords.getDisposePerson());
//                           req.setSolveTime(dateFormat.format(alarmRecords.getDisposeTime()));
//                           mentService.alterAlarmLedger(req);
//                           mentService.delAlarmWait(req);
//                           return Mono.empty();
//                       }
//                       return Mono.empty();
//                   })
//                   .then();
//    }

}
