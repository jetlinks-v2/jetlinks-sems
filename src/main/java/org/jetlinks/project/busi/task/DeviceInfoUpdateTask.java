package org.jetlinks.project.busi.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.TimerSpec;
import org.jetlinks.pro.cluster.reactor.FluxCluster;
import org.jetlinks.project.busi.abutment.abutmentService;
import org.jetlinks.project.busi.abutment.req.EquipmentReq;
import org.jetlinks.project.busi.entity.DeviceInfoEntity;
import org.jetlinks.project.busi.micro.RequestMicroHandle;
import org.jetlinks.project.busi.service.DeviceService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PreDestroy;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceInfoUpdateTask implements CommandLineRunner{

    private final abutmentService service;

    private final RequestMicroHandle requestMicroHandle;

    private final DeviceService deviceService;

    private final QueryHelper queryHelper;

    public Flux<String> selectNameByUsername(String username){
        return queryHelper
            .select("select name from user",String::new)
            .where(e->e.is("username",username))
            .fetch();

    }

    public Flux<Object> updateDeviceInfo(){
        //获取设备生命周期的设备
        return service.getDeviceInfo(new EquipmentReq())
            .flatMap(device->{
                //获取设备负责人
                if(device.getHead()!=null){
                    return this.selectNameByUsername(device.getHead())
                        .flatMap(value->{
                            if(value != null){
                                device.setHead(value);
                                return Mono.just(device);
                            }else {
                                return Mono.just(device);
                            }
                        });

                }else {
                    return Mono.just(device);
                }
            }).flatMap(device->{
                return deviceService
                    .createUpdate()
                    //设备名称
                    .set(DeviceInfoEntity::getDeviceName,device.getEquipmentName())
                    //设备负责人
                    .set(DeviceInfoEntity::getDuty,device.getHead())
                    //设备位置
                    .set(DeviceInfoEntity::getAreaId,device.getEquipmentLocation())
                    //出厂编码
                    .set(DeviceInfoEntity::getFactoryNumber,device.getFactoryCode())
                    .where(DeviceInfoEntity::getDeviceId, device.getEquipmentCode())
                    .execute();
            });
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
                .schedule("device_update_task", TimerSpec.cron("0 0 0/3 * * ?"), Flux.defer(this::updateDeviceInfo));
    }
}
