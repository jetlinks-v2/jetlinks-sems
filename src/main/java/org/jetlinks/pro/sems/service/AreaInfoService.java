package org.jetlinks.pro.sems.service;


import lombok.AllArgsConstructor;
import org.hswebframework.utils.StringUtils;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.crud.service.GenericReactiveTreeSupportCrudService;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.pro.sems.entity.dto.AreaInfoDto;
import org.jetlinks.pro.sems.entity.AreaInfoEntity;
import org.jetlinks.pro.sems.entity.DeviceInfoEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AreaInfoService extends GenericReactiveTreeSupportCrudService<AreaInfoEntity,String> {
    private final QueryHelper queryHelper;

    private final DeviceService deviceService;
    @Override
    public IDGenerator<String> getIDGenerator() {
        return IDGenerator.UUID;
    }

    @Override
    public void setChildren(AreaInfoEntity areaInfo, List<AreaInfoEntity> list) {
        areaInfo.setChildren(list);
    }

    public Mono<List<AreaInfoEntity>> getRegionTree(String id, Integer energyType) {
        QueryParamEntity queryParam=new QueryParamEntity();
        queryParam.setWhere("id ="+id+"");
        return this.queryIncludeChildrenTree(queryParam)
            .flatMap(value->{
                //获取总表区域
                return deviceService
                    .createQuery()
                    .where(DeviceInfoEntity::getParentFlag,"1")
                    .not(DeviceInfoEntity::getStatus,"1")
                    .where(DeviceInfoEntity::getEnergyType,energyType)
                    .fetch()
                    .mapNotNull(DeviceInfoEntity::getParentId)
                    .distinct()
                    .collectList()
                    .defaultIfEmpty(new ArrayList<>())
                    .flatMapMany(list->{
                        List<AreaInfoEntity> areaInfoEntities = this.screenTree(value, list);
                        return Flux.fromIterable(areaInfoEntities);
                    }).collectList();

            });

    }


    /**
     * 树形筛选查找
     *
     * @param treeDtoList 树形集合
     * @param idList      筛选条件(这里是所有绑定总表的区域)
     * @return 包含的节点数据
     */
    public static List<AreaInfoEntity> screenTree(List<AreaInfoEntity> treeDtoList, List<String> idList) {
        //最后返回的筛选完成的集合
        List<AreaInfoEntity> screeningOfCompleteList = new ArrayList<>();
        if (listIsNotEmpty(treeDtoList) ) {
            for (AreaInfoEntity treeDto : treeDtoList) {
                List<AreaInfoEntity> subsetList = treeDto.getChildren();
                //递归筛选完成后的返回的需要添加的数据
                List<AreaInfoEntity> addTreeDto = getSubsetPmsPlanPo(treeDto, subsetList, idList);

                screeningOfCompleteList.addAll(addTreeDto);

            }
            return screeningOfCompleteList;
        }
        return treeDtoList;
    }

    /**
     * 筛选符合的集合并返回
     *
     * @param treeDto           树形类
     * @param subsetTreeDtoList 子集集合
     * @param idList            筛选条件
     * @return 筛选成功的类
     */
    public static List<AreaInfoEntity> getSubsetPmsPlanPo(AreaInfoEntity treeDto, List<AreaInfoEntity> subsetTreeDtoList, List<String> idList) {
        //作为筛选条件的判断值
        String areaId = treeDto.getId();

        //有子集时,并且没有绑定总表继续向下寻找
        if (listIsNotEmpty(subsetTreeDtoList) && !idList.contains(areaId) ) {
            ArrayList<AreaInfoEntity> areaInfoEntities = new ArrayList<>();
            //添加父节点
            areaInfoEntities.add(treeDto);
            for (AreaInfoEntity subsetTreeDto : subsetTreeDtoList) {
                //获取子集
                List<AreaInfoEntity> subsetList = subsetTreeDto.getChildren();
                List<AreaInfoEntity> newTreeDto = getSubsetPmsPlanPo(subsetTreeDto, subsetList, idList);
                areaInfoEntities.addAll(newTreeDto);
            }
            return areaInfoEntities;
        }
        ArrayList<AreaInfoEntity> areaInfoEntities = new ArrayList<>();
        areaInfoEntities.add(treeDto);
        return areaInfoEntities;
    }

    /**
     * 判断集合为空
     *
     * @param list 需要判断的集合
     * @return 集合为空时返回 true
     */
    public static boolean listIsEmpty(Collection list) {
        return (null == list || list.size() == 0);
    }

    /**
     * 判断集合非空
     *
     * @param list 需要判断的集合
     * @return 集合非空时返回 true
     */
    public static boolean listIsNotEmpty(Collection list) {
        return !listIsEmpty(list);
    }

    /**
     * 判断对象为null或空时
     *
     * @param object 对象
     * @return 对象为空或null时返回 true
     */
    public static boolean isEmpty(Object object) {
        if (object == null) {
            return (true);
        }
        if ("".equals(object)) {
            return (true);
        }
        if ("null".equals(object)) {
            return (true);
        }
        return (false);
    }

    /**
     * 判断对象非空
     *
     * @param object 对象
     * @return 对象为非空时返回 true
     */
    public static boolean isNotEmpty(Object object) {
        if (object != null && !object.equals("") && !object.equals("null")) {
            return (true);
        }
        return (false);
    }

    /**
     * 递归生成树
     */
    public static List<AreaInfoEntity> createTree(String pCode, List<AreaInfoEntity> areaInfos) {
        List<AreaInfoEntity> treeMenu = new ArrayList<>();

        for (AreaInfoEntity areaInfo : areaInfos) {

            if (pCode.equals(areaInfo.getParentId())) {
                treeMenu.add(areaInfo);
                areaInfo.setChildren(createTree(areaInfo.getId(), areaInfos));
            }
        }
        return treeMenu;
    }



    /**获取所有区域下所有水电气设备id
     *<pre>{@code
     *
     *}</pre>
     * @param
     * @return
     * @see
     */
    public Flux<String> getAllAreaDeviceIds(){
        return queryHelper.select(" SELECT\n" +
                                      "    id\n" +
                                      " FROM\n" +
                                      "    area_info\n" +
                                      " WHERE\n" +
                                      "    parent_id = '0';",AreaInfoEntity::new)
                          .fetch()
                          .flatMap(i ->getDeviceIds(i.getId(),"water").collectList()
                                                                      .flatMapMany(waterDeviceIds ->
                                                                          getDeviceIds(i.getId(),"gas").collectList()
                                                                                                       .flatMapMany(gasDeviceIds ->
                                                                                                           getDeviceIds(i.getId(),"electricity").collectList()
                                                                                                                                        .flatMapMany(electricityDeviceIds -> {
                                                                                                                                            electricityDeviceIds
                                                                                                                                                .addAll(waterDeviceIds);
                                                                                                                                            electricityDeviceIds
                                                                                                                                                .addAll(gasDeviceIds);
                                                                                                                                            return Flux
                                                                                                                                                .fromIterable(electricityDeviceIds);
                                                                                                                                        })
                                                                                                       )
                                                                      )
                          );

    }


    /**获取所有区域下所有type能源类型设备id，包括绑表的表id，和未绑表的叶子节点区域的设备id
     *<pre>{@code
     *
     *}</pre>
     * @param
     * @return
     * @see
     */
    public Flux<String> getAllAreaDeviceIds(String type){
        return queryHelper.select(" SELECT\n" +
                                      "    id\n" +
                                      " FROM\n" +
                                      "    area_info\n" +
                                      " WHERE\n" +
                                      "    parent_id = '0';",AreaInfoEntity::new)
                          .fetch()
                          .flatMap(i ->getDeviceIds(i.getId(),type));

    }



    /**获取该区域下所有设备id，包括绑表的表id，和未绑表的叶子节点区域的设备id
     *<pre>{@code
     *
     *}</pre>
     * @param
     * @return
     * @see
     */
    public  synchronized Flux<String> getDeviceIds(String areaId,String type){
        QueryParamEntity queryParamEntity = new QueryParamEntity();
        queryParamEntity.setWhere("id = "+ "'"+areaId+"'");
        return this.queryIncludeChildrenTree(queryParamEntity)
                   .flatMapMany(root -> depthFirstSearch(root.get(0),type))
                   .collectList()
                   .flatMapMany(areaList -> {
                       //如果没有就返回-1
                       if(areaList.size()==0 || areaList.isEmpty()) Flux.just("-1");
                       List<String> deleteIds = new ArrayList<>();
                       //绑表的areaId
                       List<String> tableAreaList = new ArrayList<>();
                       for (AreaInfoEntity value : areaList) {
                           if(!StringUtils.isNullOrEmpty(value.getRemark()) && value.getRemark().equals("true")){
                               deleteIds.add(value.getId());
                               tableAreaList.add(value.getId());
                           }
                       }
                       deleteIds.forEach(id ->areaList.remove(id));
                       //没绑表的areaId
                       List<String> noTableAreaList = areaList
                           .stream()
                           .map(i -> i.getId())
                           .collect(Collectors.toList());
                       //查询没绑表的areaId的设备id
                       return queryHelper.select("SELECT sdi.`device_id` as `deviceId` \n" +
                                                     "FROM sems_device_info AS sdi \n" +
                                                     "LEFT JOIN area_info AS a ON a.id = sdi.area_id;", AreaInfoDto::new)
                                         .where(dsl -> {
                                             if(noTableAreaList.isEmpty()) dsl.is("sdi.area_id","-1");
                                             else dsl.in("sdi.area_id",noTableAreaList);
                                             dsl.is("energy_type",type);
                                             dsl.is("sdi.status",0);
                                             dsl.is("sdi.parent_id",0);
                                         })
                                         .fetch()
                                         .collectList()
                                         .flatMapMany(areaInfoDtoList -> {
                                             List<String> noTableDeviceIds = areaInfoDtoList
                                                 .stream()
                                                 .map(i -> i.getDeviceId())
                                                 .collect(Collectors.toList());
                                             //查询绑表的areaId的设备id
                                             return queryHelper.select("SELECT sdi.device_id as deviceId \n" +
                                                                           "FROM sems_device_info AS sdi\n" +
                                                                           "LEFT JOIN area_info AS a \n" +
                                                                           "ON a.id = sdi.parent_id",AreaInfoDto::new)
                                                               .where(dsl -> {
                                                                   if(tableAreaList.isEmpty() || tableAreaList.size()==0) dsl.is("sdi.area_id","-1");
                                                                   else dsl.in("sdi.parent_id",tableAreaList);
                                                                   dsl.is("sdi.status",0);
                                                                   dsl.is("energy_type",type);
                                                               })
                                                               .fetch()
                                                               .collectList()
                                                               .flatMapMany(tableAreaInfoDtoList -> {
                                                                   List<String> tableDeviceIds = tableAreaInfoDtoList
                                                                       .stream()
                                                                       .map(i -> i.getDeviceId())
                                                                       .collect(Collectors.toList());
                                                                   List<String> deviceIds = new ArrayList<>();
                                                                   deviceIds.addAll(noTableDeviceIds);
                                                                   deviceIds.addAll(tableDeviceIds);
                                                                   return Flux.fromIterable(deviceIds.stream().distinct().collect(Collectors.toList()));
                                                               });
                                         });
                   });
    }



    /**深度优先遍历
    *<pre>{@code
    *
    *}</pre>
    * @param
    * @return
    * @see
    */
    public Flux<AreaInfoEntity> depthFirstSearch(AreaInfoEntity root,String type) {
        //1.处理当前节点，生成节点ID
       return Flux.just(root).flatMap(areaInfo -> {
           if(areaInfo.getState().equals("1"))
               return Flux.empty();
           Flux<AreaInfoEntity> current = Flux.just(areaInfo);
           //2.1绑定判定
            if(areaInfo.getBindState()!=null && areaInfo.getBindState().equals("1")){
                return this.queryHelper.select("SELECT sdi.device_id AS deviceId\n" +
                                            "FROM area_info AS a \n" +
                                            "INNER JOIN sems_device_info as sdi \n" +
                                            "ON a.id = sdi.parent_id", AreaInfoDto::new)
                                .where(dsl -> dsl.is("a.id", areaInfo.getId())
                                                 .is("energy_type", type)
                                                 .is("sdi.status",0))
                                .count().flatMapMany(count -> {
                    if(count>0)  {
                        //2.1.1 count>0说明帮了总表，直接返回，并把孩子节点设为空，不往下继续走
                        return current.doOnNext(i -> {
                            i.setRemark("true");
                            i.setChildren(null);
                        });
                    }
                    else {
                        //2.1.1没有绑定总表，继续走
                        if (areaInfo.getChildren() != null) {
                            Flux<AreaInfoEntity> childrens = Flux.fromIterable(areaInfo.getChildren())
                                                                 .flatMap(children -> depthFirstSearch(children,type));
                            return Flux.concat(current, childrens);
                        }else {
                            return current;
                        }
                    } });
            }
            //2.2没有绑定直接继续往下走
            else {
                if (areaInfo.getChildren() != null) {
                    Flux<AreaInfoEntity> childrens = Flux.fromIterable(areaInfo.getChildren())
                                                         .flatMap(children -> depthFirstSearch(children,type));
                    return Flux.concat(current, childrens);
                }else {
                    //2.2.1 没有
                    return current;
                }
            }
        });
    }



}
