package org.jetlinks.project.busi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.utils.StringUtils;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.annotation.DeleteAction;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.pro.io.excel.ExcelUtils;
import org.jetlinks.project.busi.entity.EnergyRatioEntity;
import org.jetlinks.project.busi.entity.TestConfigDeviceEntity;
import org.jetlinks.project.busi.service.EnergyRatioService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.pro.io.excel.ImportHelper;
import org.jetlinks.pro.io.file.FileInfo;
import org.jetlinks.pro.io.file.FileManager;
import org.jetlinks.pro.io.file.FileOption;
import org.jetlinks.pro.io.utils.FileUtils;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.TestConfigEntity;
import org.jetlinks.project.busi.service.TestConfigDeviceService;
import org.jetlinks.project.busi.service.TestConfigService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;


@RestController
@RequestMapping("/test/config")
@AllArgsConstructor
@Getter
@Tag(name = "试验条目 1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "test-config", name = "试验条目")
public class TestConfigController implements AssetsHolderCrudController<TestConfigEntity,String> {

    private final TestConfigService service;

    private final TestConfigDeviceService deviceService;

    private final QueryHelper queryHelper;

    private final FileManager fileManager;

    private final TestConfigDeviceService testConfigDeviceService;

    private final EnergyRatioService energyRatioService;

    @Operation(summary = "删除试验条目")
    @PostMapping("_delete/config")
    @DeleteAction
    public Mono<Integer> deleteNew(@RequestBody TestConfigEntity entity) {
        return testConfigDeviceService
            .createQuery()
            .where(TestConfigDeviceEntity::getConfigId,entity.getId())
            .fetch()
            .hasElements()
            .flatMap(aBoolean -> {
                if(aBoolean){
                    return Mono.error(new UnsupportedOperationException("该试验条目已绑定设备，不可删除！"));
                }
                return service
                    .createUpdate()
                    .set(TestConfigEntity::getStatus,"1")
                    .where(TestConfigEntity::getId,entity.getId())
                    .execute()
                    .flatMap(e->{
                        if(e > 0){
                            return energyRatioService
                                .createUpdate()
                                .set(EnergyRatioEntity::getStatus,"1")
                                .where(EnergyRatioEntity::getConfigId,entity.getId())
                                .execute();
                        }
                        return Mono.just(e);
                    });
            });


    }

    //根据上传的文件来导入数据并将导入结果保存到文件中返回结果文件地址，
    //客户端可以引导用户下载结果文件
    @Operation(summary = "导入试验条目")
    @PostMapping("/_import.{format}")
    @ResourceAction(id= Permission.ACTION_IMPORT,name = "导入")
    public Mono<String> importByFileUpload(@PathVariable String format,
                                           @RequestPart("file") Mono<FilePart> file) {

        return FileUtils
            .dataBufferToInputStream(file.flatMapMany(FilePart::content))
            .flatMap(inputStream -> new ImportHelper<>(
                TestConfigEntity::new,
                //数据处理逻辑
                flux -> flux.doOnNext(ValidatorUtils::tryValidate).flatMap(e ->service
                    .createQuery()
                    .where(TestConfigEntity::getTestName,e.getTestName())
                    .and(TestConfigEntity::getStatus,"0")
                    .fetch()
                    .collectList()
                    .flatMap(list ->{
                        if(!StringUtils.isNullOrEmpty(e.getTestName()) && list.size() > 0){
                            return Mono.error(new RuntimeException("试验配置已存在，请重新输入！"));
                        }
                        return service.save(e);
                    })
                ).then())
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
                ExcelUtils.write(TestConfigEntity.class, Flux.empty(), format)
            );
    }


}
