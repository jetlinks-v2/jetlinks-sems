package org.jetlinks.pro.sems.controller;




import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.pro.sems.entity.res.CostconfigTimeRes;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.jetlinks.pro.sems.entity.CostConfigEntity;
import org.jetlinks.pro.sems.entity.CostNameAndRemarkEntity;
import org.jetlinks.pro.sems.entity.ElectricityIntervalEntity;
import org.jetlinks.pro.sems.service.CostConfService;
import org.jetlinks.pro.sems.service.CostNameAndRemarkService;
import org.jetlinks.pro.sems.service.ElectricityIntervalService;
import org.jetlinks.pro.sems.utils.TimeSegmentUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/cost/conf")
@AllArgsConstructor
@Getter
@Tag(name = "1.0 费用配置") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Slf4j
@Component
@Resource(id = "cost-conf", name = "费用配置")
public class CostConfController implements AssetsHolderCrudController<CostConfigEntity,String> {

    private final CostConfService service;

    private final QueryHelper queryHelper;

    private final ElectricityIntervalService electricityIntervalService;

    private final CostNameAndRemarkService costNameAndRemarkService;




    @Operation(summary = "费用配置保存")
    @PostMapping("/save")
    @Transactional
    @SaveAction
    public Mono<Void> electrilityConfSave(@RequestBody CostConfigEntity costConfigEntity) {

        CostConfigEntity costConfigEntityEmpty = new CostConfigEntity();
        //先更新费用配置名称
        CostNameAndRemarkEntity costNameAndRemarkEntity = new CostNameAndRemarkEntity();
        costNameAndRemarkEntity.setName(costConfigEntity.getCostName());
        costNameAndRemarkEntity.setRemark(costConfigEntity.getCostRemark());
        return costNameAndRemarkService
            .updateById(costConfigEntity.getCostConfigNameId(),costNameAndRemarkEntity)
            .flatMap(va->{
                if (!"2".equals(costConfigEntity.getEnergyType())) {

                    return service.createQuery()
                        .where(CostConfigEntity::getCostConfigNameId,costConfigEntity.getCostConfigNameId())
                        .where(CostConfigEntity::getEnergyType,costConfigEntity.getEnergyType())
                        .fetch()
                        .switchIfEmpty(Mono.just(costConfigEntityEmpty))
                        .flatMap(i->{
                            if(i.getId()==null){
                                return service.save(costConfigEntity.getWaterOrGasList());
                            }else {
                                List<CostConfigEntity> waterOrGasList = costConfigEntity.getWaterOrGasList();
                                //先插入id为空的
                                List<CostConfigEntity> saveData = waterOrGasList.stream().filter(j -> j.getId() == null).collect(Collectors.toList());
                                List<String> stringStream = waterOrGasList.stream().map(CostConfigEntity::getId).collect(Collectors.toList());
                                Map<String, CostConfigEntity> collect = waterOrGasList.stream().collect(Collectors.toMap(CostConfigEntity::getId, Function.identity()));


                                CostConfigEntity costConfigEntity1 = collect.get(null);

                                return service.save(saveData)
                                    .flatMap(in->{
                                        if(stringStream.contains(i.getId())){
                                            CostConfigEntity costConfigEntityU = collect.get(i.getId());
                                            costConfigEntityU.setId(i.getId());
                                            return service.updateById(i.getId(),costConfigEntityU);
                                        }else {
                                            return service.deleteById(i.getId());
                                        }
                                    });

                            }




//                         costConfigEntity.setId(i.getId());
//                         return service.updateById(i.getId(),costConfigEntity);
                            //}
                        })
                        .then();
                } else {
                    return service.createQuery()
                        .where(CostConfigEntity::getCostConfigNameId,costConfigEntity.getCostConfigNameId())
                        .where(CostConfigEntity::getEnergyType,costConfigEntity.getEnergyType())
                        .fetchOne()
                        .switchIfEmpty(Mono.just(costConfigEntityEmpty))
                        .flatMap(i->{
                            if(i.getId()==null){
                                return service.insert(costConfigEntity)
                                    .flatMap(l->{
                                        List<ElectricityIntervalEntity> electricityIntervalEntities = costConfigEntity.getElectricityIntervalEntities();
                                        for (ElectricityIntervalEntity electricityIntervalEntity : electricityIntervalEntities) {
                                            electricityIntervalEntity.setCostConfigId(costConfigEntity.getId());
                                        }
                                        return electricityIntervalService.save(costConfigEntity.getElectricityIntervalEntities());
                                    });
                            }else {

                                List<ElectricityIntervalEntity> electricityIntervalEntities = costConfigEntity.getElectricityIntervalEntities();
                                //costConfigEntity.setId(i.getId());
                                for (ElectricityIntervalEntity electricityIntervalEntity : electricityIntervalEntities) {
                                    electricityIntervalEntity.setCostConfigId(i.getId());
                                }
                                return service.updateById(i.getId(), costConfigEntity)
                                    .then(electricityIntervalService.createDelete()
                                        .where(ElectricityIntervalEntity::getCostConfigId,i.getId())
                                        .execute()
                                        .then(electricityIntervalService.save(electricityIntervalEntities))
                                    );
                            }
                        }).then();
                }
            });
    }

    @Operation(summary = "费用配置查询配置")
    @PostMapping("/get/info")
    @QueryAction
    public Mono<CostConfigEntity> electrilityConfCheck(@RequestBody CostConfigEntity costConfigEntity) {
        //获取名称和描述
        CostConfigEntity costConfigEntityRe = new CostConfigEntity();
        return costNameAndRemarkService
            .findById(costConfigEntity.getCostConfigNameId())
            .flatMap(va->{
                costConfigEntityRe.setCostName(va.getName());
                costConfigEntityRe.setCostRemark(va.getRemark());
                if ("2".equals(costConfigEntity.getEnergyType())) {
                    //电
                    return service.createQuery()
                        .where(CostConfigEntity::getEnergyType, costConfigEntity.getEnergyType())
                        .where(CostConfigEntity::getCostConfigNameId, costConfigEntity.getCostConfigNameId())
                        .fetchOne()
                        .flatMap(l -> {
                            return electricityIntervalService.createQuery()
                                .where(ElectricityIntervalEntity::getCostConfigId, l.getId())
                                .fetch()
                                .collectList()
                                .flatMap(k -> {
                                    l.setElectricityIntervalEntities(k);
                                    l.setCostName(va.getName());
                                    l.setCostRemark(va.getRemark());
                                    return Mono.just(l);
                                });

                        })
                        .switchIfEmpty(Mono.just(costConfigEntityRe));
                }else {
                    return service.createQuery()
                        .where(CostConfigEntity::getEnergyType, costConfigEntity.getEnergyType())
                        .where(CostConfigEntity::getCostConfigNameId, costConfigEntity.getCostConfigNameId())
                        .fetch()
                        .collectList()
                        .flatMap(list->{
//                    CostConfigEntity costConfigEntityReturn = list.get(0);
//                    costConfigEntityReturn.setWaterOrGasList(list);
//                    return Mono.just(costConfigEntityReturn);
                            return service.createQuery()
                                .where(CostConfigEntity::getEnergyType, costConfigEntity.getEnergyType())
                                .where(CostConfigEntity::getCostConfigNameId, costConfigEntity.getCostConfigNameId())
                                .fetchOne()
                                .doOnNext(value->value.setWaterOrGasList(list))
                                .doOnNext(value->value.setCostRemark(va.getRemark()))
                                .doOnNext(value->value.setCostName(va.getName()));

                        })
                        .switchIfEmpty(Mono.just(costConfigEntityRe));
                }
            });

    }

    @GetMapping("/getRemark")
    @Operation(summary = "能耗看板尖峰平谷描述")
    @Authorize(ignore = true)
    public Mono<CostConfigEntity> electrilityRemark() {
        //查询费用配置名称
        return costNameAndRemarkService
            .createQuery()
            .fetchOne()
            .flatMap(va->{
                return service.createQuery()
                    .where(CostConfigEntity::getEnergyType, "2")
                    .where(CostConfigEntity::getCostConfigNameId, va.getId())
                    .fetchOne()
                    .flatMap(l -> {
                        return electricityIntervalService.createQuery()
                            .where(ElectricityIntervalEntity::getCostConfigId, l.getId())
                            .fetch()
                            .collectList()
                            .flatMap(k -> {
                                l.setElectricityIntervalEntities(k);
                                l.setCostName(va.getName());
                                l.setCostRemark(va.getRemark());
                                return Mono.just(l);
                            });

                    });
            });

    }


    /**
     * 思路：将有交集的情况列出,若不符合有交集的情况,则无交集
     * 有交集的两种情况
     * 1.第一个时间段的开始时间在第二个时间段的开始时间和结束时间当中
     * 2.第二个时间段的开始时间在第一个时间段的开始时间和结束时间当中
     * 判断两个时间段是否有交集
     *
     * @param leftStartDate  第一个时间段的开始时间
     * @param leftEndDate    第一个时间段的结束时间
     * @param rightStartDate 第二个时间段的开始时间
     * @param rightEndDate   第二个时间段的结束时间
     * @return 若有交集, 返回true, 否则返回false
     */
    public boolean hasOverlap(Date leftStartDate, Date leftEndDate, Date rightStartDate, Date rightEndDate) {

        return ((leftStartDate.getTime() >= rightStartDate.getTime())
            && leftStartDate.getTime() < rightEndDate.getTime())
            ||
            ((leftStartDate.getTime() > rightStartDate.getTime())
                && leftStartDate.getTime() <= rightEndDate.getTime())
            ||
            ((rightStartDate.getTime() >= leftStartDate.getTime())
                && rightStartDate.getTime() < leftEndDate.getTime())
            ||
            ((rightStartDate.getTime() > leftStartDate.getTime())
                && rightStartDate.getTime() <= leftEndDate.getTime());

}
    private ArrayList<String> compareTwoArraySame(String[] arr1, String[] arr2) {

        Set<String> testSet = new HashSet<>();

        for (int i = 0; i<arr1.length; i++){
            testSet.add(arr1[i]);
        }
        ArrayList<String> repeat = new ArrayList<>();

        for(int i = 0; i < arr2.length; i++){
            if( !testSet.add(arr2[i])){//set集合中如果存入的值已经存在则会返回false
                repeat.add(arr2[i]);
            }
        }
        return repeat;
    }

    /**通过configID删除*/
    public Mono<Integer> del(String id){
          return electricityIntervalService.createDelete()
            .where(ElectricityIntervalEntity::getCostConfigId,id)
            .execute();
    }

    /**解析时间*/
    public Date getDateByString(String date){
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date parse =null;
        try {
            parse = dateFormat.parse(date);

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return parse;
    }


    /**时间检验公共方法*/
    public Integer judgeTime(CostconfigTimeRes costconfigTimeRes , List<CostconfigTimeRes> totalCostconfigTimeResList){
        int flag=0;
            //获取开始时间
            String startDate = costconfigTimeRes.getStartDate();
            Date start = this.getDateByString(startDate);
            //获取结束时间
            String endDate = costconfigTimeRes.getEndDate();
            Date end = this.getDateByString(endDate);
            for (CostconfigTimeRes timeResAll : totalCostconfigTimeResList) {
                //获取开始时间
                String startString = timeResAll.getStartDate();
                Date startTime = this.getDateByString(startString);
                //获取结束时间
                String endString = timeResAll.getEndDate();
                Date endTime = this.getDateByString(endString);
                boolean startAfter = start.after(startTime) ;

                boolean startBefore = start.before(endTime) ;

                boolean endAfter = end.after(startTime)  ;
                boolean endBefore = end.before(endTime) ;
                if ((startAfter && startBefore)  ){
                    flag++;
                }
                if(endAfter && endBefore ){
                    flag++;
                }
                if(start.equals(startTime) && end.equals(endTime)){
                    flag++;
                }
                if((startTime.equals(start) && endTime.before(end)) || startTime.after(start) && endTime.equals(end)){
                    flag++;
                }
            }
            return flag;

        }

        @PostMapping("/check")
        @Operation(summary = "检查电的时间是否重复")
        @Authorize(ignore = true)
        public Mono<Boolean> check(@RequestBody CostConfigEntity costConfigEntity){
            ElectricityIntervalEntity electricityIntervalEntity = new ElectricityIntervalEntity();
            List<ElectricityIntervalEntity> electricityIntervalEntities = costConfigEntity.getElectricityIntervalEntities();
            //先检查自身
            return this.checkSelf(costConfigEntity)
                .then(
             Flux.fromIterable(electricityIntervalEntities)
                .flatMap(p->{
                    if(p.getCuspPeriods()!=null){
                        //检查里面的时段
                        List<CostconfigTimeRes> cuspPeriods = p.getCuspPeriods();
                        List<CostconfigTimeRes> flatPeriods = p.getFlatPeriods();
                        List<CostconfigTimeRes> peakPeriods = p.getPeakPeriods();
                        List<CostconfigTimeRes> valleyPeriods = p.getValleyPeriods();
                        ArrayList<CostconfigTimeRes> all = new ArrayList<>();
                        all.addAll(cuspPeriods);
                        all.addAll(flatPeriods);
                        all.addAll(peakPeriods);
                        all.addAll(valleyPeriods);

                        //判断平时
                        for (CostconfigTimeRes flatPeriod : flatPeriods) {
                            Integer integer= this.judgeTime(flatPeriod, all);
                            if(integer>1){
                                return Mono.error(new UnsupportedOperationException("平时时段重复！"));
                            }

                        }
                        //判断尖时
                        for (CostconfigTimeRes cuspPeriod : cuspPeriods) {
                            Integer integer = this.judgeTime(cuspPeriod, all);
                            if(integer>1){
                                return Mono.error(new UnsupportedOperationException("尖峰时段重复！"));
                            }
                        }

                        //判断峰时
                        for (CostconfigTimeRes peakPeriod : peakPeriods) {
                            Integer integer=this.judgeTime(peakPeriod, all);
                            if(integer>1){
                                return Mono.error(new UnsupportedOperationException("高峰时段重复！"));
                            }

                        }


                        //判断谷时
                        for (CostconfigTimeRes valleyPeriod : valleyPeriods) {
                            Integer integer = this.judgeTime(valleyPeriod, all);
                            if (integer > 1) {
                                return Mono.error(new UnsupportedOperationException("谷时时段重复！"));
                            }
                        }
                    }
                    return electricityIntervalService.createQuery()
                        .not(ElectricityIntervalEntity::getCostConfigId,costConfigEntity.getId())
                        .where(ElectricityIntervalEntity::getState,"1")
                        .nest()
                        .between(ElectricityIntervalEntity::getYearStart,p.getYearStart(),p.getYearEnd())
                        .or()
                        .between(ElectricityIntervalEntity::getYearEnd,p.getYearStart(),p.getYearEnd())
                        .end()
                        .fetch()
                        .switchIfEmpty(Mono.just(electricityIntervalEntity))
                        .flatMap(k->{
                            if(k.getMonth()==null){
                                return Mono.just(Boolean.FALSE);
                            }else {
                                String month1 = p.getMonth();
                                String[] split = month1.split(",");
                                String month = k.getMonth();
                                String[] split1 = month.split(",");
                                ArrayList<String> s = this.compareTwoArraySame(split1, split);
                                if(!s.isEmpty()){
                                    return service.findById(k.getCostConfigId())
                                        .flatMap(o->{
                                            return costNameAndRemarkService.findById(o.getCostConfigNameId())
                                                .flatMap(j->{
                                                    String join = String.join(",", s);
                                                    return Mono.error( new UnsupportedOperationException("电生效时间段月份与:"+j.getName()+"电费配置生效时间 "+join+"月份存在重复重复！请检查后重新保存"));
                                                });
                                        });

                                }else {
                                    return Mono.just(Boolean.FALSE);
                                }
                            }

                });
        }).then(Mono.just(Boolean.FALSE))
            );
        }


        /**检查该配置下面的生效时间是否重复*/
        public Mono<Boolean> checkSelf(CostConfigEntity costConfigEntity){
            List<ElectricityIntervalEntity> electricityIntervalEntities = costConfigEntity.getElectricityIntervalEntities();

            //先判断年是否重复
            if(electricityIntervalEntities.size()<=1){
                return Mono.just(Boolean.FALSE);
            }else {
                return Flux.fromIterable(electricityIntervalEntities)
                    .flatMap(p->{
                        Boolean flag=Boolean.FALSE;
                        List<ElectricityIntervalEntity> collect = electricityIntervalEntities.stream().filter(i -> !i.equals(p)).collect(Collectors.toList());
                        for (ElectricityIntervalEntity k : collect) {
                            if ((p.getYearStart() >= k.getYearStart() && p.getYearStart() <= k.getYearEnd()) || (p.getYearEnd() >= k.getYearStart() && p.getYearEnd() <= k.getYearEnd())) {
                                //年重复
                                String month1 = p.getMonth();
                                String[] split = month1.split(",");
                                String month = k.getMonth();
                                String[] split1 = month.split(",");
                                if (!this.compareTwoArraySame(split1, split).isEmpty()) {
                                    flag = Boolean.TRUE;
                                }
                            }
                        }
                        if(flag){
                            return Mono.error(new UnsupportedOperationException("该配置的生效时间段存在重复，请检查后保存！"));
                        }else {
                            return Mono.just(Boolean.FALSE);
                        }
                    }).then(Mono.just(Boolean.FALSE));
            }

//
        }


        public Mono<Boolean> checkGasOrElectricity( CostConfigEntity costConfigEntity){

            CostConfigEntity costConfigEntityEmpty = new CostConfigEntity();
            return service.createQuery()
                .where(CostConfigEntity::getEnergyType,costConfigEntity.getEnergyType())
                .where(CostConfigEntity::getState,"1")
                .not(CostConfigEntity::getCostConfigNameId,costConfigEntity.getCostConfigNameId())
                .nest()
                .between(CostConfigEntity::getEffectiveTimeIntervalStartDate,costConfigEntity.getEffectiveTimeIntervalStartDate(),costConfigEntity.getEffectiveTimeIntervalEndDate())
                .or()
                .between(CostConfigEntity::getEffectiveTimeIntervalEndDate,costConfigEntity.getEffectiveTimeIntervalStartDate(),costConfigEntity.getEffectiveTimeIntervalEndDate())
                .orNest()
                .lte(CostConfigEntity::getEffectiveTimeIntervalStartDate,costConfigEntity.getEffectiveTimeIntervalStartDate())
                .and()
                .gte(CostConfigEntity::getEffectiveTimeIntervalEndDate,costConfigEntity.getEffectiveTimeIntervalEndDate())
                .end()
                .end()
                .fetchOne()
                .switchIfEmpty(Mono.just(costConfigEntityEmpty))
                .flatMap(value-> {
                    if(value.getId()==null){
                        return Mono.just(Boolean.FALSE);
                    }
                        return costNameAndRemarkService
                            .findById(value.getCostConfigNameId())
                            .flatMap(v->{
                                 return Mono.error(new UnsupportedOperationException("该配置的生效时间区间与配置名称为:"+v.getName()+" 相对应的能源的配置存在重复！请查看该配置后重新保存！")
                                 );
                            });

                });
        }

    @PostMapping("/check/other")
    @Operation(summary = "检验水气的时间是否重复")
    @Authorize(ignore = true)
    public Mono<Boolean> checkWa(@RequestBody CostConfigEntity costConfigEntity){
//检查自身
        List<CostConfigEntity> waterOrGasList = costConfigEntity.getWaterOrGasList();
        ArrayList<TimeSegmentUtil> timeSegmentUtils = new ArrayList<>();
        for (CostConfigEntity configEntity : waterOrGasList) {
            TimeSegmentUtil timeSegmentUtil = new TimeSegmentUtil(configEntity.getEffectiveTimeIntervalStartDate(),configEntity.getEffectiveTimeIntervalEndDate());
            timeSegmentUtils.add(timeSegmentUtil);
        }
        long sum = timeSegmentUtils.stream().sorted(TimeSegmentUtil::compareTo).mapToLong(TimeSegmentUtil::getOverlapCounter).sum();
        if(sum>0){
            return Mono.error(new UnsupportedOperationException("该配置的生效时间区间重复！"));
        }
        return Flux.fromIterable(waterOrGasList)
            .flatMap(value->{
                return this.checkGasOrElectricity(value);
            })
            .collectList()
            .flatMap(va->{
                    return Mono.just(Boolean.FALSE);
            });
    }


}
