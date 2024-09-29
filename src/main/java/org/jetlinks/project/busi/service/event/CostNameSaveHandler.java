package org.jetlinks.project.busi.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.events.EntityBeforeCreateEvent;
import org.hswebframework.web.crud.events.EntityBeforeSaveEvent;
import org.jetlinks.project.busi.entity.CostNameAndRemarkEntity;
import org.jetlinks.project.busi.service.CostNameAndRemarkService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CostNameSaveHandler {

    private  final CostNameAndRemarkService costNameAndRemarkService;

    @EventListener
    public void handleCreatedEvent(EntityBeforeCreateEvent<CostNameAndRemarkEntity> event){

        event.async(this.sendCreatedNotify(event.getEntity()));

    }



    public Mono<Void> sendCreatedNotify(List<CostNameAndRemarkEntity> costNameAndRemarkEntities) {

        return Flux
            .fromIterable(costNameAndRemarkEntities)
            .flatMap(e -> {
                    //判断是否存在相同的名称
                    return costNameAndRemarkService
                        .createQuery()
                        .where(CostNameAndRemarkEntity::getName,e.getName())
                        .where(CostNameAndRemarkEntity::getState,"1")
                        .fetch()
                        .hasElements()
                        .flatMap(booleans->{
                            if(booleans) {
                                return Mono.error(new UnsupportedOperationException("存在重复费用配置名称！"));
                            }
                            return Mono.empty();
                        });
                })
            .then();
    }
}
