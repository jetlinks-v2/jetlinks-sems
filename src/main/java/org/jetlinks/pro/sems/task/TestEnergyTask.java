package org.jetlinks.pro.sems.task;

import lombok.RequiredArgsConstructor;
import org.jetlinks.pro.TimerSpec;
import org.jetlinks.pro.cluster.reactor.FluxCluster;
import org.jetlinks.pro.sems.entity.TestRecordEntity;
import org.jetlinks.pro.sems.service.TestRecordService;
import org.jetlinks.pro.sems.service.event.TestRecordEventHandler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TestEnergyTask implements CommandLineRunner {

    private static HashMap<String, Disposable> disposableHashMap = new HashMap<>();

    private final TestRecordEventHandler testRecordEventHandler;

    private final TestRecordService service;

    public Mono<Void> getVoidMono(TestRecordEntity testRecordEntity){
        Disposable disposable = disposableHashMap.get(testRecordEntity.getId());
        if(Objects.nonNull(disposable)){
            disposable.dispose();
            disposableHashMap.remove(testRecordEntity.getId());
        }
        disposableHashMap.remove(testRecordEntity.getId());
        if (testRecordEntity.getItemStatus().equals("1")) {
            disposable = FluxCluster.schedule(testRecordEntity.getId(), TimerSpec.cron("0 0/10 * * * ?"),
                          Mono.defer(() -> testRecordEventHandler
                              .energyCostStat(Collections.singletonList(testRecordEntity))
                          ));
            disposableHashMap.put(testRecordEntity.getId(),disposable);
        }
        return Mono.empty();
    }

    @Override
    public void run(String... args) throws Exception {
        service.createQuery()
               .where(TestRecordEntity::getItemStatus,"1")
               .fetch()
               .flatMap(this::getVoidMono)
               .subscribe();
    }
}
