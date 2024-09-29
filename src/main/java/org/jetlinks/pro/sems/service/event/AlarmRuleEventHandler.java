package org.jetlinks.pro.sems.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.events.EntityBeforeModifyEvent;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.jetlinks.pro.sems.entity.AlarmRuleEntity;
import org.jetlinks.pro.sems.service.AlarmRuleService;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class AlarmRuleEventHandler {

    private final AlarmRuleService service;

    private final ReactiveRedisTemplate<Object, Object> redis;

    @EventListener
    public void handleCreatedEvent(EntityCreatedEvent<AlarmRuleEntity> event){

        event.async(this.sendCreatedNotify(event.getEntity()));

    }

    @EventListener
    public void handleUpdateEvent(EntityModifyEvent<AlarmRuleEntity> event){

        event.async(this.sendUpdateNotify(event.getAfter()));

    }

    @EventListener
    public void handleBeforeUpdateEvent(EntityBeforeModifyEvent<AlarmRuleEntity> event){

        event.async(this.sendBeforeUpdateNotify(event.getBefore()));

    }

    public Mono<Void> sendCreatedNotify(List<AlarmRuleEntity> alarmRuleList){

        return Flux
            .fromIterable(alarmRuleList)
            .flatMap(e -> {
                if(Objects.isNull(e.getRuleType())){
                    return redis
                        .opsForValue()
                        .set("Rule:" + e.getAlarmTypeId() + e.getAlarmType() + e.getEnergyType(),e);
                } else {
                    return redis
                        .opsForValue()
                        .set("Rule:" + e.getAlarmTypeId() + e.getAlarmType() + e.getEnergyType() + e.getRuleType(),e);
                }
            })
            .then();
    }

    public Mono<Void> sendUpdateNotify(List<AlarmRuleEntity> alarmRuleList){

        return Flux
            .fromIterable(alarmRuleList)
            .flatMap(e -> {
                if(e.getStatus().equals("1")){
                    if(Objects.isNull(e.getRuleType())){
                        return redis.delete("Rule:" + e.getAlarmTypeId() + e.getAlarmType() + e.getEnergyType(),e);
                    } else {
                        return redis.delete("Rule:" + e.getAlarmTypeId() + e.getAlarmType() + e.getEnergyType() + e.getRuleType(), e);
                    }
                } else {
                    if(Objects.isNull(e.getRuleType())){
                        return redis.opsForValue().set("Rule:" + e.getAlarmTypeId() + e.getAlarmType() + e.getEnergyType(),e);
                    } else {
                        return redis.opsForValue().set("Rule:" + e.getAlarmTypeId() + e.getAlarmType() + e.getEnergyType() + e.getRuleType(), e);
                    }

                }
            })
            .then();
    }

    public Mono<Void> sendBeforeUpdateNotify(List<AlarmRuleEntity> alarmRuleList){
        return Flux
            .fromIterable(alarmRuleList)
            .flatMap(e->{
                return service
                    .findById(e.getId())
                    .flatMap(alarmRuleEntity -> {
                        if(Objects.isNull(e.getRuleType())){
                            return redis.delete("Rule:" + e.getAlarmTypeId() + e.getAlarmType() + e.getEnergyType(),e);
                        } else {
                            return redis.delete("Rule:" + e.getAlarmTypeId() + e.getAlarmType() + e.getEnergyType() + e.getRuleType(), e);
                        }
                    });
            }).then();
    }

}
