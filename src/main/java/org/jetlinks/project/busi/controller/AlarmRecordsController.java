package org.jetlinks.project.busi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.pro.io.excel.ExcelUtils;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.AlarmRecordsEntity;
import org.jetlinks.project.busi.entity.req.AlarmRecordsReq;
import org.jetlinks.project.busi.entity.res.AlarmRecordsExcelRes;
import org.jetlinks.project.busi.service.AlarmRecordsService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@RestController
@RequestMapping("/alarm/records")
@AllArgsConstructor
@Getter
@Tag(name = "告警记录 1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "energy-alarm-records", name = "能耗告警记录")
public class AlarmRecordsController implements AssetsHolderCrudController<AlarmRecordsEntity,String> {

    private final AlarmRecordsService service;

    private final QueryHelper queryHelper;

    @Operation(summary = "批量确认")
    @PutMapping("/_batch/confirm")
    @ResourceAction(id= Permission.ACTION_UPDATE,name = "告警确认")
    public Mono<Integer> batchConfirm(@RequestBody @Validated AlarmRecordsReq alarmRecordsReq) {
        String ids = alarmRecordsReq.getIds();
        String disposeResult = alarmRecordsReq.getDisposeResult();
        if(StringUtils.isNotEmpty(ids) && ids.contains(",")){
            String[] id = ids.split(",");
            return Authentication
                .currentReactive()
                .switchIfEmpty(Mono.error(UnAuthorizedException::new))
                .flatMap(auth -> service.createUpdate()
                                    .set(AlarmRecordsEntity::getDisposeResult,disposeResult)
                                    .set(AlarmRecordsEntity::getDisposeTime,new Date())
                                    .set(AlarmRecordsEntity::getDisposePerson, auth.getUser().getName())
                                    .set(AlarmRecordsEntity::getStatus, 1)
                                    .in(AlarmRecordsEntity::getId,id)
                                    .execute());
        } else {
            String id = ids;
            return Authentication
                .currentReactive()
                .switchIfEmpty(Mono.error(UnAuthorizedException::new))
                .flatMap(auth -> service.createUpdate()
                                    .set(AlarmRecordsEntity::getDisposeResult,disposeResult)
                                    .set(AlarmRecordsEntity::getDisposeTime,new Date())
                                    .set(AlarmRecordsEntity::getDisposePerson, auth.getUser().getName())
                                    .set(AlarmRecordsEntity::getStatus, 1)
                                    .in(AlarmRecordsEntity::getId,id)
                                    .execute()
                                    .flatMap(e->{
                                        if(e > 0){
                                            return service.findById(id)
                                                          .flatMap(entity -> service.createUpdate()
                                                                                .set(AlarmRecordsEntity::getDisposeResult,disposeResult)
                                                                                .set(AlarmRecordsEntity::getDisposeTime,new Date())
                                                                                .set(AlarmRecordsEntity::getDisposePerson, auth.getUser().getName())
                                                                                .set(AlarmRecordsEntity::getStatus, 1)
                                                                                .where(AlarmRecordsEntity::getRuleId,entity.getRuleId())
                                                                                .and(AlarmRecordsEntity::getStatus,"0")
                                                                                .execute());
                                        }
                                        return Mono.just(e);
                                    }));
        }
    }

    @Operation(summary = "查询告警记录")
    @PostMapping("/query/list")
    @Authorize(ignore = true)
    public Flux<AlarmRecordsEntity> queryList(@RequestBody QueryParamEntity query) {
        return service.createQuery().setParam(query).fetch();
    }

    @Operation(summary = "导出告警记录")
    @GetMapping("/_export/{name}.{format}")
    @Authorize(ignore = true)
    @ResourceAction(id= Permission.ACTION_EXPORT,name = "导出")
    public Mono<Void> export(QueryParamEntity param,
                             @PathVariable String name,
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
                ExcelUtils.write(AlarmRecordsExcelRes.class, queryHelper
                                     .select("SELECT alarm_code as alarmCode,rule_name as ruleName,\n" +
                                                 "CASE\n" +
                                                 "WHEN alarm_type = 0 THEN '设备'\n" +
                                                 "WHEN alarm_type = 1 THEN '试验'\n" +
                                                 "ELSE '场所'\n" +
                                                 "END AS alarmType,\n" +
                                                 "alarm_content as alarmContent,\n" +
                                                 "alarm_time as alarmTime,\n" +
                                                 "dispose_person as disposePerson,\n" +
                                                 "dispose_time as disposeTime,\n" +
                                                 "dispose_result as disposeResult\n" +
                                                 "FROM sems_alarm_records",AlarmRecordsExcelRes::new)
                                     .where(dsl -> dsl.setParam(param))
                                     .fetch(),
                                 format)
            );
    }
}
