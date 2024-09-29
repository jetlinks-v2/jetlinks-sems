package org.jetlinks.project.busi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.DeviceInfoEntity;
import org.jetlinks.project.busi.entity.ElectricityConsumeEntity;
import org.jetlinks.project.busi.entity.req.EnergyConsumeReq;
import org.jetlinks.project.busi.entity.res.ElectricityConsumeLossRes;
import org.jetlinks.project.busi.entity.res.GasConsumeLossRes;
import org.jetlinks.project.busi.enums.EnergyType;
import org.jetlinks.project.busi.service.DeviceService;
import org.jetlinks.project.busi.service.ElectricityConsumeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/sems/electricity/consume")
@AllArgsConstructor
@Getter
@Tag(name = "用电信息1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "electricity-consume", name = "用电信息")
public class ElectricityConsumeController implements AssetsHolderCrudController<ElectricityConsumeEntity,String> {

    private final ElectricityConsumeService service;

    private final QueryHelper queryHelper;

    private final DeviceService deviceService;



    public Flux<Object> queryLossElectricity(EnergyConsumeReq req){
        if (req.getDateType().equals(1)) {
            return queryHelper
                .select("SELECT\n" +
                            "FROM_UNIXTIME(ROUND(gather_time/1000,0),\"%Y-%m-%d %H:%i\") as gatherTime,\n" +
                            "sum( difference ) difference\n" +
                            "FROM\n" +
                            "sems_electricity_consume \n" +
                            "GROUP BY FROM_UNIXTIME(ROUND(gather_time/1000,0),\"%Y-%m-%d %H:%i\")", ElectricityConsumeLossRes::new)
                .where(req.getQuery())
                .fetch()
                .collectList()
                .flatMapMany(data -> Flux.just(data));
        } else {
            return queryHelper
                .select("SELECT\n" +
                            "DATE(FROM_UNIXTIME(ROUND(gather_time/1000,0))) as gatherTime,\n" +
                            "sum( difference ) difference\n" +
                            "FROM\n" +
                            "sems_electricity_consume \n" +
                            "GROUP BY DATE(FROM_UNIXTIME(ROUND(gather_time/1000,0)))", ElectricityConsumeLossRes::new)
                .where(req.getQuery())
                .fetch()
                .distinct()
                .collectList()
                .flatMapMany(list -> {
                    //所有时间集合
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                    //获取两个时间之间的所有时间集合
                    List<String> timeList = new ArrayList<>();
                    Calendar calendar = Calendar.getInstance();
                    Date startDate =null;
                    Date endDate =null;
                    try {
                        startDate = format.parse(format.format(req.getBeginTime()));
                        endDate = format.parse(format.format(req.getEndTime()));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    while (startDate.getTime() <= endDate.getTime()) {
                        //把日期添加进集合
                        timeList.add(format.format(startDate));
                        //设置日期
                        calendar.setTime(startDate);
                        //把日期增加一条
                        calendar.add(Calendar.DATE, 1);
                        //获取增加后的日期
                        startDate = calendar.getTime();
                    }
                    List<ElectricityConsumeLossRes> returnList = new LinkedList<>();
                    returnList.addAll(list);
                    //遍历Map进行数据添加
                    for(String time:timeList){
                        //判断是否具有该时间的数据
                        boolean result = returnList.stream().anyMatch(e -> e.getGatherTime().equals(time));
                        //如果没有就进行添加
                        if(!result){
                            ElectricityConsumeLossRes energy = new ElectricityConsumeLossRes();
                            energy.setGatherTime(time);
                            energy.setDifference(new BigDecimal(0));
                            returnList.add(energy);
                        }
                    }
                    returnList = returnList.stream().sorted(Comparator.comparing(e -> e.getGatherTime())).collect(Collectors.toList());
                    return Flux.just(returnList);
                });
        }
    }

    @Operation(summary = "查询用电能耗详情")
    @Authorize(ignore = true)
    @PostMapping("/_query/loss/electricity")
    @QueryAction
    public Mono<List<HashMap<String,Object>>> addReportDevice(@RequestBody EnergyConsumeReq req){
        //返回体

        if(req.getDeviceId() ==null){
            HashMap<String, Object> map = new HashMap<>();
            ArrayList<HashMap<String,Object>> objects = new ArrayList<>();
            return this.queryLossElectricity(req)
                .doOnNext(value->map.put("list",value))
                .doOnNext(value->objects.add(map))
                .then(Mono.just(objects));
        }
        //查对应的表
        return deviceService
            .createQuery()
            .where(DeviceInfoEntity::getParentId,req.getDeviceId())
            .where(DeviceInfoEntity::getStatus,"0")
            .where(DeviceInfoEntity::getEnergyType, EnergyType.electricity)
            .fetch()
            .flatMap( v->{
                HashMap<String, Object> resultMap = new HashMap<>();
                resultMap.put("reportDevice",v.getDeviceId());
                    return this.queryLossElectricity(req)
                        .doOnNext(value->resultMap.put("list",value))
                        .then(Mono.just(resultMap));
            }).collectList();
    }
}
