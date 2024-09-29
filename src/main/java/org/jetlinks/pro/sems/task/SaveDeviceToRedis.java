package org.jetlinks.pro.sems.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.pro.sems.entity.DeviceInfoEntity;
import org.jetlinks.pro.sems.service.DeviceService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SaveDeviceToRedis implements CommandLineRunner {
    private final DeviceService deviceService;
    private final ReactiveRedisTemplate<Object,Object> redis;

    @Override
    public void run(String... args) throws Exception {
        //查询设备列表
         deviceService.createQuery()
            .where(DeviceInfoEntity::getStatus,0)
            .fetch()
            .flatMap(device-> redis.opsForValue().set("device:"+device.getDeviceId(), device)).subscribe();
    }
}
