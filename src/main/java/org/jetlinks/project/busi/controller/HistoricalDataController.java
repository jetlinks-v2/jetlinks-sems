package org.jetlinks.project.busi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.project.busi.asset.ExampleAssetType;
import org.jetlinks.project.busi.entity.DeviceInfoEntity;
import org.jetlinks.project.busi.entity.req.HistoryDataReq;
import org.jetlinks.project.busi.entity.res.HistoryDataInfo;
import org.jetlinks.project.busi.service.AreaInfoService;
import org.jetlinks.project.busi.service.DeviceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.0
 */
@RestController
@RequestMapping("/sems/history/data")
@AllArgsConstructor
@Getter
@Tag(name = "历史数据查询1.0") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
@Resource(id = "history-data", name = "历史数据")
public class HistoricalDataController {

    private final QueryHelper queryHelper;
    private final DeviceService deviceService;
    private final AreaInfoService areaInfoService;

    @Operation(summary = "历史数据查询")
    @PostMapping("/query")
    @QueryAction
    public Flux<PagerResult<HistoryDataInfo>> historyDataQuery(@RequestBody HistoryDataReq historyDataReq) {
        return fetchHistoryData(historyDataReq);
    }


    private Flux<PagerResult<HistoryDataInfo>> fetchHistoryData(HistoryDataReq historyDataReq) {
        Long startTime;
        Long endTime;
        //1.判断是否默认，默认情况今天到前30天
        if (historyDataReq.getStartDate() != null && historyDataReq.getEndDate() != null) {
            startTime = historyDataReq.getStartDate();
            endTime = historyDataReq.getEndDate();
        } else {
            endTime = System.currentTimeMillis();
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -30);
            Date thirtyDaysAgo = calendar.getTime();
            startTime = thirtyDaysAgo.getTime();
        }
        String energyTypeName = historyDataReq.getEnergyType().getValue();
        String sql;
        if (energyTypeName.equals("electricity")) {
        sql="SELECT \n" +
            "    sdi.device_name AS deviceName,\n" +
            "    a.area_name AS areaName,\n" +
            "    FROM_UNIXTIME((ec.gather_time) / 1000, '%Y-%m-%d') AS statisticsDate,\n" +
            "    CASE WHEN SUM(CASE WHEN ec.periods_type=1 THEN ec.difference END)>0 THEN SUM(CASE WHEN ec.periods_type=1 THEN ec.difference END) ELSE 0 END cuspNumber,\n" +
            "    CASE WHEN SUM(CASE WHEN ec.periods_type=2 THEN ec.difference END)>0 THEN SUM(CASE WHEN ec.periods_type=2 THEN ec.difference END) ELSE 0 END peakNumber,\n" +
            "    CASE WHEN SUM(CASE WHEN ec.periods_type=3 THEN ec.difference END)>0 THEN SUM(CASE WHEN ec.periods_type=3 THEN ec.difference END) ELSE 0 END flatNumber,\n" +
            "    CASE WHEN SUM(CASE WHEN ec.periods_type=4 THEN ec.difference END)>0 THEN SUM(CASE WHEN ec.periods_type=3 THEN ec.difference END) ELSE 0 END valleyNumber\n" +
            "FROM\n" +
            "    sems_electricity_consume ec\n" +
            "LEFT JOIN\n" +
            "    sems_device_info sdi ON sdi.device_id = ec.device_id\n" +
            "LEFT JOIN\n" +
            "    area_info a  ON a.id = sdi.area_id\n" +
            "GROUP BY\n" +
            "    sdi.device_id,\n" +
            "    sdi.device_name,\n" +
            "    a.area_name,\n" +
            "    FROM_UNIXTIME((gather_time) / 1000, '%Y-%m-%d')\n" +
            "ORDER BY\n" +
            "    statisticsDate DESC,deviceName ASC;";
    }else {
        sql="SELECT\n" +
            "    sdi.device_name AS deviceName,\n" +
            "    a.area_name AS areaName,\n" +
            "    FROM_UNIXTIME((ec.gather_time) / 1000, '%Y-%m-%d') AS statisticsDate,\n" +
            "    CASE WHEN MIN(ec.number-ec.difference) >0 THEN MIN(ec.number-ec.difference) ELSE 0 END startNumber,\n" +
            "    CASE WHEN MAX(ec.number) > 0 THEN MAX(ec.number) ELSE 0 END endNumber,\n" +
            "    CASE WHEN (MAX(ec.number)-MIN(ec.number-ec.difference))> 0 THEN MAX(ec.number)-MIN(ec.number-ec.difference) ELSE 0 END totalNumber\n" +
            "FROM\n" +
            "    sems_"+energyTypeName+"_consume ec\n" +
            "LEFT JOIN\n" +
            "    sems_device_info sdi ON sdi.device_id = ec.device_id\n" +
            "LEFT JOIN\n" +
            "    area_info a  ON a.id = sdi.area_id\n" +
            "GROUP BY\n" +
            "    sdi.device_id,\n" +
            "    sdi.device_name,\n" +
            "    a.area_name,\n" +
            "    FROM_UNIXTIME((gather_time) / 1000, '%Y-%m-%d')\n" +
            "ORDER BY\n" +
            "    statisticsDate DESC,deviceName ASC;";
        }

        return  deviceService
            .createQuery()
            .where(dsl -> {
                dsl.$like$(DeviceInfoEntity::getDeviceName,historyDataReq.getDeviceName());
                dsl.nest(ds -> ds.or("parent_id",0)
                                 .or("parent_flag",1));
            })
            .fetch()
            .distinct(s -> s.getDeviceId())
            .map(deviceInfo -> deviceInfo.getDeviceId())
            .collectList()
            .flatMapMany(
                values -> queryHelper.select(sql,HistoryDataInfo::new)
                                    .where(dsl -> {
                                        QueryParamEntity queryParamEntity = new QueryParamEntity();
                                        queryParamEntity.setPageSize(historyDataReq.getPageSize());
                                        queryParamEntity.setPageIndex(historyDataReq.getPageIndex());
                                        dsl.setParam(queryParamEntity);
                                       dsl.$like$("device_name",historyDataReq.getDeviceName());
                                        dsl.in("device_id",values);
                                        dsl.between("gather_time", startTime, endTime);
                                    }).fetchPaged());
    }
    /**
     *
     * @param list  要分页的集合
     * @param pageNo    第几页
     * @param pageSize  每页条数
     * @return      分页集合对象
     */
    public static List page(List list, int pageNo, int pageSize){

        List result = new ArrayList();
        if (list != null && list.size() > 0) {
            int allCount = list.size();
            int pageCount = (allCount + pageSize - 1) / pageSize;
            if (pageNo >= pageCount) {
                pageNo = pageCount;
            }
            int start = (pageNo - 1) * pageSize;
            int end = pageNo * pageSize;
            if (end >= allCount) {
                end = allCount;
            }
            for (int i = start; i < end; i++) {
                result.add(list.get(i));
            }
        }
        return (result.size() > 0) ? result : null;
    }

}
