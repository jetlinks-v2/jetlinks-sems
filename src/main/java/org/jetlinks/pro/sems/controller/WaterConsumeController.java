package org.jetlinks.pro.sems.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.pro.sems.entity.req.EnergyConsumeReq;
import org.jetlinks.pro.sems.entity.res.WaterConsumeLossRes;
import org.jetlinks.pro.sems.entity.res.WaterConsumeRes;
import org.jetlinks.pro.sems.asset.ExampleAssetType;
import org.jetlinks.pro.sems.entity.DeviceInfoEntity;
import org.jetlinks.pro.sems.entity.WaterConsumeEntity;
import org.jetlinks.pro.sems.enums.EnergyType;
import org.jetlinks.pro.sems.service.DeviceService;
import org.jetlinks.pro.sems.service.WaterConsumeService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/sems/water/consume")
@AllArgsConstructor
@Getter
@Tag(name = "用水信息1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "water-consume", name = "用水信息")
public class WaterConsumeController implements AssetsHolderCrudController<WaterConsumeEntity,String> {

    private final WaterConsumeService service;

    private final QueryHelper queryHelper;

    private final DeviceService deviceService;


    @Operation(summary = "查询带设备名称的用水信息数据")
    @PostMapping("/_query/withDeviceName")
    @QueryAction
    public Flux<WaterConsumeRes> queryWithDeviceName(@RequestBody QueryParamEntity query){
        return queryHelper
            .select("SELECT A.*,B.device_name deviceName \n" +
                "FROM sems_water_consume A\n" +
                "LEFT JOIN sems_device_info B\n" +
                "ON A.device_id = B.device_id",WaterConsumeRes::new)
            .where(query)
            .fetch()
            .flatMap(data -> {
                BigDecimal number = data.getNumber();
                BigDecimal difference = data.getDifference();
                data.setBeginNumber(number.subtract(difference));
                return Flux.just(data);
            });
    }



    public Flux<Object> queryLossWater(EnergyConsumeReq req){
        if (req.getDateType().equals(1)) {
            return queryHelper
                .select("SELECT\n" +
                            "FROM_UNIXTIME(ROUND(gather_time/1000,0),\"%Y-%m-%d %H:%i\") as gatherTime,\n" +
                            "sum( difference ) difference\n" +
                            "FROM\n" +
                            "sems_water_consume \n" +
                            "GROUP BY FROM_UNIXTIME(ROUND(gather_time/1000,0),\"%Y-%m-%d %H:%i\")", WaterConsumeLossRes::new)
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
                            "sems_water_consume \n" +
                            "GROUP BY DATE(FROM_UNIXTIME(ROUND(gather_time/1000,0)))", WaterConsumeLossRes::new)
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
                    List<WaterConsumeLossRes> returnList = new LinkedList<>();
                    returnList.addAll(list);
                    //遍历Map进行数据添加
                    for(String time:timeList){
                        //判断是否具有该时间的数据
                        boolean result = returnList.stream().anyMatch(e -> e.getGatherTime().equals(time));
                        //如果没有就进行添加
                        if(!result){
                            WaterConsumeLossRes energy = new WaterConsumeLossRes();
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

    @Operation(summary = "查询用水能耗详情")
    @Authorize(ignore = true)
    @PostMapping("/_query/loss/water")
    @QueryAction
    public Mono<List<HashMap<String,Object>>> addReportDevice(@RequestBody EnergyConsumeReq req){
        //返回体

        //查对应的表
        return deviceService
            .createQuery()
            .where(DeviceInfoEntity::getParentId,req.getDeviceId())
            .where(DeviceInfoEntity::getStatus,"0")
            .where(DeviceInfoEntity::getEnergyType, EnergyType.water)
            .fetch()
            .flatMap( v->{
                HashMap<String, Object> resultMap = new HashMap<>();
                resultMap.put("reportDevice",v.getDeviceId());
                return this.queryLossWater(req)
                    .doOnNext(value->resultMap.put("list",value))
                    .then(Mono.just(resultMap));
            }).collectList();
    }
}
