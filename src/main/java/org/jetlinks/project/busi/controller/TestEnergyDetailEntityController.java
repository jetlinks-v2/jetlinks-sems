package org.jetlinks.project.busi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.TestEnergyDetailEntity;
import org.jetlinks.project.busi.entity.res.TestEnergyDetailRes;
import org.jetlinks.project.busi.service.TestEnergyDetailService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/test/energy/detail")
@AllArgsConstructor
@Getter
@Tag(name = "试验能耗详情 1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "test-energy-detail", name = "试验能耗详情")
public class TestEnergyDetailEntityController implements AssetsHolderCrudController<TestEnergyDetailEntity,String> {

    private final TestEnergyDetailService service;

    private final QueryHelper queryHelper;

    @Operation(summary = "能耗详情")
    @GetMapping("/_energy/_detail")
    @QueryAction
    public Flux<TestEnergyDetailRes> getEnergyDetailSum(String recordIds) {
        if(StringUtils.isNotEmpty(recordIds) && recordIds.contains(",")) {
            String[] recordId = recordIds.split(",");
            return queryHelper.select("SELECT\n" +
                                          "ted.test_record_id as testRecordId,\n" +
                                          "tr.name,\n" +
                                          "tr.test_start_time as testStartTime,\n" +
                                          "tr.test_end_time as testEndTime,\n" +
                                          "IFNULL(sum(ted.water),0) as water,\n" +
                                          "IFNULL(sum(ted.electricity),0) as electricity,\n" +
                                          "IFNULL(sum(ted.gas),0) as gas,\n" +
                                          "IFNULL(sum(ted.water_price),0) as waterPrice,\n" +
                                          "IFNULL(sum(ted.electricity_price),0) as electricityPrice,\n" +
                                          "IFNULL(sum(ted.gas_price),0) as gasPrice\n" +
                                          "FROM sems_test_energy_detail ted LEFT JOIN sems_test_record tr on ted.test_record_id = tr.id\n" +
                                          "GROUP BY test_record_id", TestEnergyDetailRes::new)
                              .where(dsl -> dsl.in("test_record_id", recordId))
                              .fetch()
                              .flatMap(e->{
                                  return service
                                      .createQuery()
                                      .where()
                                      .in(TestEnergyDetailEntity::getTestRecordId,e.getTestRecordId())
                                      .orderBy(SortOrder.desc(TestEnergyDetailEntity::getDeviceId))
                                      .fetch()
                                      .collectList()
                                      .flatMap(list ->{
                                          e.setEnergyDetailList(list);
                                          return Mono.just(e);
                                      });
                              });
        } else {
            return queryHelper.select("SELECT\n" +
                                          "ted.test_record_id as testRecordId,\n" +
                                          "tr.name,\n" +
                                          "tr.test_start_time as testStartTime,\n" +
                                          "tr.test_end_time as testEndTime,\n" +
                                          "IFNULL(sum(ted.water),0) as water,\n" +
                                          "IFNULL(sum(ted.electricity),0) as electricity,\n" +
                                          "IFNULL(sum(ted.gas),0) as gas,\n" +
                                          "IFNULL(sum(ted.water_price),0) as waterPrice,\n" +
                                          "IFNULL(sum(ted.electricity_price),0) as electricityPrice,\n" +
                                          "IFNULL(sum(ted.gas_price),0) as gasPrice\n" +
                                          "FROM sems_test_energy_detail ted LEFT JOIN sems_test_record tr on ted.test_record_id = tr.id\n" +
                                          "GROUP BY test_record_id", TestEnergyDetailRes::new)
                              .where(dsl -> dsl.and("test_record_id", recordIds))
                              .fetch()
                              .flatMap(e->{
                                  return service
                                      .createQuery()
                                      .where()
                                      .in(TestEnergyDetailEntity::getTestRecordId,e.getTestRecordId())
                                      .orderBy(SortOrder.desc(TestEnergyDetailEntity::getDeviceId))
                                      .fetch()
                                      .collectList()
                                      .flatMap(list ->{
                                          e.setEnergyDetailList(list);
                                          return Mono.just(e);
                                      });
                              });
    }

    }


}
