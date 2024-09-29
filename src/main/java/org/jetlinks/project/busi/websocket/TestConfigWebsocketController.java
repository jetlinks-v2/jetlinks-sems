package org.jetlinks.project.busi.websocket;

import org.hswebframework.utils.StringUtils;
import org.hswebframework.web.validator.CreateGroup;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.pro.gateway.external.SubscribeRequest;
import org.jetlinks.pro.gateway.external.SubscriptionProvider;
import org.jetlinks.pro.io.excel.AbstractImporter;
import org.jetlinks.pro.io.excel.ImportHelper;
import org.jetlinks.pro.io.file.FileManager;
import org.jetlinks.project.busi.entity.TestConfigEntity;
import org.jetlinks.project.busi.service.TestConfigService;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class TestConfigWebsocketController extends AbstractImporter<TestConfigEntity> implements SubscriptionProvider {

    private final TestConfigService testConfigService;

    public TestConfigWebsocketController(TestConfigService service,
                                     FileManager fileManager,
                                     WebClient.Builder client) {
        super(fileManager, client.build());
        this.testConfigService = service;
    }

    @Override
    public String id() {
        return "testConfig-import";
    }

    @Override
    public String name() {
        return "导入数据";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/testConfig-import"
        };
    }

    @Override
    public Flux<ImportResult<TestConfigEntity>> subscribe(SubscribeRequest request) {
        String url = request
            .getString("fileUrl")
            .orElseThrow(() -> new IllegalArgumentException("fileUrl can not be null"));
        return doImport(url);
    }

    @Override
    protected void customImport(ImportHelper<TestConfigEntity> helper) {
        super.customImport(helper);

        //批量操作数量
        helper.bufferSize(200);

        //当批量操作失败后回退到单个数据进行操作
        helper.fallbackSingle(true);

        //读取数据后校验数据
        helper.afterReadValidate(CreateGroup.class);
    }

    //处理数据的逻辑
    @Override
    protected Mono<Void> handleData(Flux<TestConfigEntity> data) {
        return testConfigService
            .save(data)
            .then();
    }

    @Override
    protected TestConfigEntity newInstance() {
        return new TestConfigEntity();
    }
}
