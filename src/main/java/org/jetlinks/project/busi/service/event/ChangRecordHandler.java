package org.jetlinks.project.busi.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.jetlinks.project.busi.entity.*;
import org.jetlinks.project.busi.service.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChangRecordHandler {

    private final ChangeRecordService service;

    private final DeviceService deviceService;

    private final ElectricityConsumeService electricityConsumeService;

    private final WaterConsumeService waterConsumeService;

    private final GasConsumeService gasConsumeService;

    @EventListener
    public void handleCreatedEvent(EntitySavedEvent<ChangeRecordEntity> event) {

        event.async(this.sendSaveNotify(event.getEntity()));

    }

    public Mono<Void> sendSaveNotify(List<ChangeRecordEntity> changeRecordList) {

        return Flux
            .fromIterable(changeRecordList)
            .flatMap(changeRecordEntity -> {
                if(changeRecordEntity.getChangeTypeEnum().getValue().equals("WRONGBINDING")){
                    return deviceService
                        .createQuery()
                        .where(DeviceInfoEntity::getParentId,changeRecordEntity.getDeviceId())
                        .and(DeviceInfoEntity::getEnergyType,changeRecordEntity.getEnergyType())
                        .not(DeviceInfoEntity::getStatus,0)
                        .orderBy(SortOrder.desc(DeviceInfoEntity::getCreateTime))
                        .fetchOne()
                        .flatMap(deviceInfoEntity -> {
                            if (deviceInfoEntity.getEnergyType()[0].getValue().equals("water")){
                                return waterConsumeService
                                    .createDelete()
                                    .where(WaterConsumeEntity::getDeviceId,changeRecordEntity.getDeviceId())
                                    .and(WaterConsumeEntity::getReportDeviceId,deviceInfoEntity.getDeviceId())
                                    .execute();
                            }
                            if (deviceInfoEntity.getEnergyType()[0].getValue().equals("electricity")){
                                return electricityConsumeService
                                    .createDelete()
                                    .where(ElectricityConsumeEntity::getDeviceId, changeRecordEntity.getDeviceId())
                                    .and(ElectricityConsumeEntity::getReportDeviceId,deviceInfoEntity.getDeviceId())
                                    .execute();
                            }
                            if (deviceInfoEntity.getEnergyType()[0].getValue().equals("gas")){
                                return gasConsumeService
                                .createDelete()
                                .where(GasConsumeEntity::getDeviceId, changeRecordEntity.getDeviceId())
                                .and(GasConsumeEntity::getReportDeviceId,deviceInfoEntity.getDeviceId())
                                .execute();
                            }
                            return Mono.empty();
                        });
                }
                return Mono.empty();
            })
            .then();
    }
}
