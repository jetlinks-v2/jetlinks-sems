package org.jetlinks.pro.sems.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.authorization.annotation.DeleteAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.jetlinks.pro.sems.entity.AlarmRuleEntity;
import org.jetlinks.pro.sems.service.AlarmRuleService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Objects;

@RestController
@RequestMapping("/alarm/rule")
@AllArgsConstructor
@Getter
@Tag(name = "告警规则 1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "energy-alarm-rule", name = "能耗告警规则")
public class AlarmRuleController implements AssetsHolderCrudController<AlarmRuleEntity,String> {

    private final AlarmRuleService service;

    private final QueryHelper queryHelper;

    @Operation(summary = "删除告警规则")
    @PostMapping("_delete/rule")
    @DeleteAction
    public Mono<Integer> deleteNew(@RequestBody AlarmRuleEntity entity) {
        return service
            .createUpdate()
            .set(AlarmRuleEntity::getStatus,"1")
            .where(AlarmRuleEntity::getId,entity.getId())
            .execute();
    }

    @Operation(summary = "新增告警规则")
    @PostMapping("/_save/_alarm/_rule")
    @SaveAction
    public Mono<Integer> saveAlarmRule(@RequestBody AlarmRuleEntity alarmRuleEntity) {

        if(Objects.isNull(alarmRuleEntity.getRuleType())){
            return service.createQuery()
                          .where(AlarmRuleEntity::getAlarmType,alarmRuleEntity.getAlarmType())
                          .and(AlarmRuleEntity::getAlarmTypeId,alarmRuleEntity.getAlarmTypeId())
                          .and(AlarmRuleEntity::getStatus,0)
                          .fetch()
                          .collectList()
                          .flatMap(e->e.size() < 1 ? service.insert(alarmRuleEntity) :
                              Mono.error(new RuntimeException((alarmRuleEntity.getAlarmType().equals("1")?"试验":"场所")
                                                                  +"告警规则已存在")));
        } else {
            return service.createQuery()
                          .where(AlarmRuleEntity::getAlarmType,alarmRuleEntity.getAlarmType())
                          .and(AlarmRuleEntity::getAlarmTypeId,alarmRuleEntity.getAlarmTypeId())
                          .and(AlarmRuleEntity::getRuleType,alarmRuleEntity.getRuleType())
                          .and(AlarmRuleEntity::getStatus,0)
                          .fetch()
                          .collectList()
                          .flatMap(e->e.size() < 1 ? service.insert(alarmRuleEntity)
                              : Mono.error(new RuntimeException("该设备的"+
                                (alarmRuleEntity.getRuleType().equals("0")?"能耗":"功率") +"告警规则已存在")));
        }

    }


}
