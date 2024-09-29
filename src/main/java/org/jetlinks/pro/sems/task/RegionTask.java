package org.jetlinks.pro.sems.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.jetlinks.pro.TimerSpec;
import org.jetlinks.pro.cluster.reactor.FluxCluster;
import org.jetlinks.pro.sems.abutment.abutmentService;
import org.jetlinks.pro.sems.abutment.res.LocationRes;
import org.jetlinks.pro.sems.entity.AreaInfoEntity;
import org.jetlinks.pro.sems.service.AreaInfoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PreDestroy;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegionTask implements CommandLineRunner {

    private final abutmentService service;

    private final AreaInfoService areaInfoService;

    public Mono<Void>  abuntData() {
        Flux<LocationRes> deviceLocation = service.getDeviceLocation();
        ArrayList<AreaInfoEntity> areaInfoEntities = new ArrayList<>();
        return deviceLocation
            .flatMap(value->{
                String locationId = value.getLocationId();
                AreaInfoEntity areaInfoEntity = new AreaInfoEntity();
                if(value.getLocationPid()!=null){
                    areaInfoEntity.setParentId(value.getLocationPid());
                }
                areaInfoEntity.setId(value.getLocationId());
                areaInfoEntity.setAreaName(value.getLocationName());
                areaInfoEntity.setAddr(value.getDetailedAddress());
                areaInfoEntity.setState(value.getEnable().toString().equals("1")?"0":"1");
                return Mono.just(areaInfoEntity);
            })
            .collectList()
            .flatMapMany(list->{
                return Flux.fromIterable(TreeSupportEntity.list2tree(list, AreaInfoEntity::setChildren)) ;
            })
            .flatMap(value->{

                return areaInfoService.save(value);

            }).then();

    }
    private Disposable disposable;

    @PreDestroy
    public void shutdown() {
        //停止定时任务
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void run(String... args) throws Exception {
        disposable =
            FluxCluster
                //不同的任务名不能相同
                .schedule("region_task", TimerSpec.cron("0 30 0/1 * * ?"),Mono.defer(this::abuntData));
    }
}

