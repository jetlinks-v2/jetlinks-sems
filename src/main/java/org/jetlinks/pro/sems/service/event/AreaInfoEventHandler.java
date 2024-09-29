package org.jetlinks.pro.sems.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.events.*;
import org.jetlinks.pro.sems.entity.AreaInfoEntity;
import org.jetlinks.pro.sems.entity.DeviceInfoEntity;
import org.jetlinks.pro.sems.service.AreaInfoService;
import org.jetlinks.pro.sems.service.DeviceService;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class AreaInfoEventHandler {


    private final ReactiveRedisTemplate<String, String> redis;

    private final DeviceService deviceService;

    private final AreaInfoService areaInfoService;


    @EventListener
    public void handleCreatedEvent(EntityCreatedEvent<AreaInfoEntity> event){

        log.info("新增监听->{}"+event);
        event.async(this.sendCreatedNotify(event.getEntity()));

    }



    public Mono<Void> sendCreatedNotify(List<AreaInfoEntity> areaInfoEntityList){

        return Flux
            .fromIterable(areaInfoEntityList)
            .flatMap(e -> {
                return redis
                .opsForValue()
                .set(e.getId(),e.getAreaName());
            })
            .then();
    }

    //删除检验
    @EventListener
    public void handleDeleteEvent(EntityBeforeDeleteEvent<AreaInfoEntity> event){
        event.async(this.sendDeleteNotify(event.getEntity()));

    }


    public Flux<Void> sendDeleteNotify(List<AreaInfoEntity> areaInfoEntityList){


        childMenu.clear();
        return Flux
            .fromIterable(areaInfoEntityList)
            .flatMap(e -> {
                //获取该区域的子区域,并判断子区域是否绑定设备
                return areaInfoService
                    .query(new QueryParamEntity())
                    .collectList()
                    .flatMapIterable(list->this.treeMenuList(list,e.getId()))
                    .flatMap(id->{
                        return deviceService
                            .createQuery()
                            .where(DeviceInfoEntity::getAreaId, id)
                            .where(DeviceInfoEntity::getParentId,"0")
                            .fetch()
                            .hasElements()
                            .flatMap(l -> {
                                if (l) {
                                    return Mono.error(new UnsupportedOperationException("该区域的子类绑定了设备，请先解绑子类再删除！"));
                                }
                                return Mono.empty();
                            });
                    })
            .then(
                //判断设备是否绑定该区域
                deviceService
                .createQuery()
                .where(DeviceInfoEntity::getAreaId, e.getId())
                    .where(DeviceInfoEntity::getParentId,"0")
                .fetch()
                .hasElements()
                .flatMap(l -> {
                    if (l) {
                        return Mono.error(new UnsupportedOperationException("该区域已被设备绑定，不可删除该区域！"));
                    }
                    return Mono.empty();
                })
    );
    });
    }


    //子节点
    static  List<String> childMenu=new ArrayList<String>();

    /**
     * 获取某个父节点下面的所有子节点
     * @param menuList
     * @param pid
     * @return
     */
    public  List<String> treeMenuList( List<AreaInfoEntity> menuList, String pid){
        for(AreaInfoEntity mu: menuList){
            //遍历出父id等于参数的id，add进子节点集合
            if(mu.getParentId().equals(pid)){
                //递归遍历下一级
                treeMenuList(menuList,mu.getId());
                childMenu.add(mu.getId());
            }
        }
        return childMenu;
    }

    //删除时也删掉redis中的数据
    @EventListener
    public void handleDeleteEvent(EntityDeletedEvent<AreaInfoEntity> event){
        event.async(this.sendDeleteAfterNotify(event.getEntity()));

    }

    public Mono<Void> sendDeleteAfterNotify(List<AreaInfoEntity> areaInfoEntityList){

        return Flux
            .fromIterable(areaInfoEntityList)
            .flatMap(e ->
                redis.delete(e.getId())
            )
            .then();
    }


    //判断树的级数是否大于5级
    @EventListener
    public void handleBeforeInsertEvent(EntityBeforeCreateEvent<AreaInfoEntity> event){
        event.async(this.sendBeforeInsertNotify(event.getEntity()));

    }

    public Mono<Void> sendBeforeInsertNotify(List<AreaInfoEntity> areaInfoEntityList){
        return Flux
            .fromIterable(areaInfoEntityList)
            .flatMap(e->{
                if(e.getLevel()>=5){
                    return Mono.error(new Throwable("该区域树结构超过5层，不能再添加子集！"));
                }
                return Mono.empty();
            }).then();
    }




}
