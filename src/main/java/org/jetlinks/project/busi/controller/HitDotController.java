package org.jetlinks.project.busi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.AlarmRecordsEntity;
import org.jetlinks.project.busi.entity.HitDotEntity;
import org.jetlinks.project.busi.entity.req.HitDotReq;
import org.jetlinks.project.busi.entity.res.HitDotRes;
import org.jetlinks.project.busi.service.AlarmRecordsService;
import org.jetlinks.project.busi.service.HitDotService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/hit/dot")
@AllArgsConstructor
@Getter
@Tag(name = "打点 1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
public class HitDotController implements AssetsHolderCrudController<HitDotEntity,String> {
    private final HitDotService service;

    private final AlarmRecordsService recordsService;

    private final QueryHelper queryHelper;

    @Operation(summary = "打点")
    @PostMapping("/hit/dot")
    @ResourceAction(id= Permission.ACTION_UPDATE, name = "更新")
    public Mono<Integer> saveHitDot(@RequestBody HitDotReq req) {

        List<HitDotEntity> hitDotList = req.getHitDotList();

        if(hitDotList.size() < 1){
            //设备ID数组长度为0时，为解绑，删除所有映射
            return service
                .createDelete()
                .where(HitDotEntity::getFloor,req.getFloor())
                .execute();
        } else {
            //先删除再添加
            return service
                .createDelete()
                .where(HitDotEntity::getFloor,req.getFloor())
                .execute()
                .then(service.insertBatch(Flux.just(hitDotList)));
        }
    }

    @Operation(summary = "查询打点")
    @GetMapping("/query/list")
    @Authorize(ignore = true)
    public Flux<HitDotRes> queryList() {
        return queryHelper
            .select("SELECT \n" +
                        "* \n" +
                        "FROM sems_hit_dot",HitDotRes::new)
            .fetch()
            .flatMap(hitDotRes -> {
                if("2".equals(hitDotRes.getType())){
                    return recordsService.createQuery()
                                         .where(AlarmRecordsEntity::getAlarmType,"0")
                                         .and(AlarmRecordsEntity::getAlarmTypeId,hitDotRes.getHouseDeviceId())
                                         .orderBy(SortOrder.desc(AlarmRecordsEntity::getAlarmTime))
                                         .fetch()
                                         .collectList()
                                         .flatMap(list ->{
                                             if(list.size() > 0){
                                                 hitDotRes.setAlarmCode(list.get(0).getAlarmCode());
                                                 hitDotRes.setAlarmStatus(list.get(0).getStatus());
                                             }
                                             return Mono.just(hitDotRes);
                                         });
                }
                return Mono.just(hitDotRes);
            });
    }
}