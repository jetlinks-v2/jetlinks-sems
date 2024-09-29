//package org.jetlinks.project.busi.task;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
//import org.jetlinks.pro.TimerSpec;
//import org.jetlinks.pro.cluster.reactor.FluxCluster;
//import org.jetlinks.project.busi.abutment.AbutmentCustService;
//import org.jetlinks.project.busi.service.TestAreaService;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//import reactor.core.Disposable;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import javax.annotation.PreDestroy;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class TestAreaTask  implements CommandLineRunner {
//
//
//    private final AbutmentCustService abutmentCustService;
//
//    private final TestAreaService testAreaService;
//
//    public Mono<SaveResult> getTestArea() {
//        return abutmentCustService.getTestSite()
//            .filter(va->!"正投影".equals(va.getAreaName()))
//            .collectList()
//            .flatMap(testAreaService::save);
//
//
//    }
//        private Disposable disposable;
//
//    @PreDestroy
//    public void shutdown() {
//        //停止定时任务
//        if (disposable != null) {
//            disposable.dispose();
//        }
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        disposable =
//            FluxCluster
//                //不同的任务名不能相同
//                .schedule("test_area_task", TimerSpec.cron("0 30 23 * * ?"), Flux.defer(this::getTestArea));
//    }
//}
