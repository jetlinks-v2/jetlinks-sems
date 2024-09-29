package org.jetlinks.pro.sems.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.utils.StringUtils;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.pro.io.excel.ExcelUtils;
import org.jetlinks.pro.io.excel.ImportHelper;
import org.jetlinks.pro.io.file.FileInfo;
import org.jetlinks.pro.io.file.FileManager;
import org.jetlinks.pro.io.file.FileOption;
import org.jetlinks.pro.io.utils.FileUtils;
import org.jetlinks.pro.sems.entity.res.TestEnergyDetailRes;
import org.jetlinks.pro.sems.service.event.TestRecordEventHandler;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.jetlinks.pro.sems.entity.TestConfigEntity;
import org.jetlinks.pro.sems.entity.TestRecordEntity;
import org.jetlinks.pro.sems.service.TestConfigService;
import org.jetlinks.pro.sems.service.TestRecordService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;


@RestController
@RequestMapping("/test/record")
@AllArgsConstructor
@Getter
@Tag(name = "试验记录 1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "test-record", name = "试验记录")
public class TestRecordController implements AssetsHolderCrudController<TestRecordEntity,String> {

    private final TestRecordService service;

    private final TestConfigService configService;

    private final QueryHelper queryHelper;

    private final FileManager fileManager;

    private final TestRecordEventHandler testRecordEventHandler;

    @Operation(summary = "修改试验记录")
    @PostMapping("/update/record")
    @ResourceAction(id= Permission.ACTION_UPDATE, name = "更新")
    public Mono<Void> updateRecord(@RequestBody TestRecordEntity testRecordEntity) {

        return service
            .updateById(testRecordEntity.getId(),testRecordEntity).then();

    }

    //导出数据
    @Operation(summary = "导入模板")
    @GetMapping("/_export")
    @ResourceAction(id= Permission.ACTION_EXPORT,name = "导出")
    public Mono<Void> export(String format,
                             ServerWebExchange exchange) {

        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_OCTET_STREAM);
        //文件名
        exchange.getResponse().getHeaders().setContentDisposition(
            ContentDisposition
                .attachment()
                .filename("模板", StandardCharsets.UTF_8)
                .build()
        );
        return exchange
            .getResponse()
            .writeWith(
                ExcelUtils.write(TestRecordEntity.class, Flux.empty(), format)
            );
    }

    //根据上传的文件来导入数据并将导入结果保存到文件中返回结果文件地址，
    //客户端可以引导用户下载结果文件
    @Operation(summary = "导入试验记录")
    @PostMapping("/_import.{format}")
    @ResourceAction(id= Permission.ACTION_IMPORT,name = "导入")
    public Mono<String> importByFileUpload(@PathVariable String format,
                                           @RequestPart("file") Mono<FilePart> file) {

        return FileUtils
            .dataBufferToInputStream(file.flatMapMany(FilePart::content))
            .flatMap(inputStream -> new ImportHelper<>(
                TestRecordEntity::new,
                //数据处理逻辑
                flux -> flux.doOnNext(ValidatorUtils::tryValidate)
                            .flatMap(e -> configService.createQuery()
                                                 .where()
                                                 .and(TestConfigEntity::getTestName,e.getTestName())
                                                 .and(TestConfigEntity::getStatus,0)
                                                 .fetch()
                                                 .collectList()
                                                 .flatMap(list ->{
                                                     if(!StringUtils.isNullOrEmpty(e.getTestName()) && list.size() == 0){
                                                         return Mono.error(new RuntimeException("试验配置不存在，请重新输入！"));
                                                     }
                                                     if(e.getTestType().equals("风阻")){
                                                         e.setTestType("1");
                                                     }
                                                     if(e.getTestType().equals("其他气动")){
                                                         e.setTestType("2");
                                                     }
                                                     if(e.getTestType().equals("风噪")){
                                                         e.setTestType("3");
                                                     }
                                                     if(e.getTestType().equals("其他声学")){
                                                         e.setTestType("4");
                                                     }
                                                     e.setConfigId(list.get(0).getId());
                                                     return service.insert(e);
                                                 }))
                            .then())
                //批量处理数量
                .bufferSize(200)
                //当批量处理失败时,是否回退到单条数据处理
                .fallbackSingle(true)
                .doImport(inputStream,
                          format,
                          //忽略导入结果
                          info -> null,
                          //将导入的结果保存为临时文件
                          result -> fileManager
                              .saveFile("import." + format, result, FileOption.tempFile)
                              .map(FileInfo::getAccessUrl))
                .last()
            );
    }

    @Operation(summary = "查询今日试验能耗")
    @GetMapping("/query/today/energy")
    @Authorize(ignore = true)
    public Flux<TestEnergyDetailRes> queryList() {
        return queryHelper
            .select("SELECT\n" +
                        "tc.test_name as name,\n" +
                        "IFNULL( sum( ted.electricity ), 0 ) AS electricity \n" +
                        "FROM\n" +
                        "sems_test_energy_detail ted\n" +
                        "LEFT JOIN sems_test_record tr ON ted.test_record_id = tr.id \n" +
                        "LEFT JOIN sems_test_config tc on tr.config_id = tc.id\n" +
                        "WHERE\n" +
                        "(DATE(NOW()) <= DATE( FROM_UNIXTIME( SUBSTR( tr.test_end_time, 1, 10 ))) AND DATE( NOW()) >= DATE(FROM_UNIXTIME(SUBSTR( tr.test_start_time, 1, 10 )))) OR \n" +
                        "(DATE(NOW()) >= DATE(FROM_UNIXTIME(SUBSTR( tr.test_start_time, 1, 10 ))) AND DATE(NOW()) <= DATE(FROM_UNIXTIME(SUBSTR( tr.test_end_time, 1, 10 )))) OR \n" +
                        "(DATE(FROM_UNIXTIME(SUBSTR( tr.test_start_time, 1, 10 ))) <= DATE(NOW()) AND ISNULL( tr.test_end_time )) \n" +
                        "GROUP BY\n" +
                        "tr.config_id", TestEnergyDetailRes::new)
            .where(dsl -> dsl.doPaging(0,5))
            .fetch();
    }

}
