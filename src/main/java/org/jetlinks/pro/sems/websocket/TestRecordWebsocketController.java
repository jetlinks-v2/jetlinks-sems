package org.jetlinks.pro.sems.websocket;

import org.hswebframework.utils.StringUtils;
import org.hswebframework.web.validator.CreateGroup;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.pro.gateway.external.SubscribeRequest;
import org.jetlinks.pro.gateway.external.SubscriptionProvider;
import org.jetlinks.pro.io.excel.AbstractImporter;
import org.jetlinks.pro.io.excel.ImportHelper;
import org.jetlinks.pro.io.file.FileManager;
import org.jetlinks.pro.sems.service.TestRecordService;
import org.jetlinks.pro.sems.entity.TestConfigEntity;
import org.jetlinks.pro.sems.entity.TestRecordEntity;
import org.jetlinks.pro.sems.service.TestConfigService;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class TestRecordWebsocketController extends AbstractImporter<TestRecordEntity> implements SubscriptionProvider {

    private final TestRecordService testRecordService;

    private final TestConfigService testConfigService;

    public TestRecordWebsocketController(TestRecordService service,
                                         TestConfigService testConfig,
                                         FileManager fileManager,
                                         WebClient.Builder client) {
        super(fileManager, client.build());
        this.testRecordService = service;
        this.testConfigService = testConfig;
    }

    @Override
    public String id() {
        return "testRecord-import";
    }

    @Override
    public String name() {
        return "导入数据";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/testRecord-import"
        };
    }

    @Override
    public Flux<ImportResult<TestRecordEntity>> subscribe(SubscribeRequest request) {
        String url = request
            .getString("fileUrl")
            .orElseThrow(() -> new IllegalArgumentException("fileUrl can not be null"));
        return doImport(url);
    }

    @Override
    protected void customImport(ImportHelper<TestRecordEntity> helper) {
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
    protected Mono<Void> handleData(Flux<TestRecordEntity> data) {
        return data.doOnNext(ValidatorUtils::tryValidate).flatMap(e ->{
            return testConfigService.createQuery()
                             .where(TestConfigEntity::getTestName,e.getTestName())
                             .and(TestConfigEntity::getStatus,0)
                             .fetch()
                             .collectList()
                             .flatMap(list ->{
                                 if(!StringUtils.isNullOrEmpty(e.getTestName()) && list.size() == 0){
                                     return Mono.error(new RuntimeException("试验配置不存在，请重新输入！"));
                                 }
                                 e.setConfigId(list.get(0).getId());
                                 return testRecordService.insert(e);
                             });
        }).then();
    }

    @Override
    protected TestRecordEntity newInstance() {
        return new TestRecordEntity();
    }
}
