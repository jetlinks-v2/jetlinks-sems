package org.jetlinks.pro.sems.websocket;

import org.hswebframework.web.validator.CreateGroup;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.pro.gateway.external.SubscribeRequest;
import org.jetlinks.pro.gateway.external.SubscriptionProvider;
import org.jetlinks.pro.io.excel.AbstractImporter;
import org.jetlinks.pro.io.excel.ImportHelper;
import org.jetlinks.pro.io.file.FileManager;
import org.jetlinks.pro.sems.entity.AreaInfoEntity;
import org.jetlinks.pro.sems.service.AreaInfoService;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class WebsocketImportController extends AbstractImporter<AreaInfoEntity> implements SubscriptionProvider {

    private final AreaInfoService areaInfoService;

    public WebsocketImportController(AreaInfoService service,
                                  FileManager fileManager,
                                  WebClient.Builder client) {
        super(fileManager, client.build());
        this.areaInfoService = service;
    }

    @Override
    public String id() {
        return "areaInfo-import";
    }

    @Override
    public String name() {
        return "导入数据";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/areaInfo-import"
        };
    }

    @Override
    public Flux<ImportResult<AreaInfoEntity>> subscribe(SubscribeRequest request) {
        String url = request
            .getString("fileUrl")
            .orElseThrow(() -> new IllegalArgumentException("fileUrl can not be null"));
        return doImport(url);
    }

    @Override
    protected void customImport(ImportHelper<AreaInfoEntity> helper) {
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
    protected Mono<Void> handleData(Flux<AreaInfoEntity> data) {
        return data.doOnNext(ValidatorUtils::tryValidate).as(areaInfoService::save).then();
    }

    @Override
    protected AreaInfoEntity newInstance() {
        return new AreaInfoEntity();
    }
}
