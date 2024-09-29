package org.jetlinks.pro.sems.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.pro.TimerSpec;
import org.jetlinks.pro.cluster.reactor.FluxCluster;
import org.jetlinks.pro.sems.entity.DeviceInfoEntity;
import org.jetlinks.pro.sems.iot.IotService;
import org.jetlinks.pro.sems.service.DeviceService;
import org.jetlinks.pro.sems.service.DeviceStateService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceComputeStatusTask implements CommandLineRunner {

    private Disposable disposable;

    private final DeviceService deviceService;

    private final DeviceStateService deviceStateService;

    private final IotService iotService;

    public Mono<Void> getDeviceComputeStatus(){
        Map<String, DeviceInfoEntity> deviceMap = new HashMap<>();
        return deviceService
            .createQuery()
            .where(DeviceInfoEntity::getStatus,"0")
            .fetch()
            .flatMap(device-> {
                String deviceStatus = iotService.getDeviceState(device.getDeviceId());
                if(!device.getDeviceStatus().equals(deviceStatus)){
                    device.setDeviceStatus(deviceStatus);
                    deviceMap.put(device.getDeviceId(),device);
                }
                return Mono.just(device);
            })
            .then(deviceService.save(deviceMap.values()))
            .then();
    }


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
                .schedule("device_compute_status_task", TimerSpec.cron("0 0 23 * * ?"), Mono.defer(this::getDeviceComputeStatus));
    }
}
