package org.jetlinks.project.busi.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.project.busi.entity.GasConsumeEntity;
import org.jetlinks.project.busi.entity.res.GasConsumeLossRes;
import org.jetlinks.project.busi.entity.res.WaterConsumeLossRes;
//import org.jetlinks.project.busi.iot.IotService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;

@Service
@AllArgsConstructor
public class GasConsumeService extends GenericReactiveCrudService<GasConsumeEntity,String> {

    private final QueryHelper queryHelper;

    private final AreaInfoService areaInfoService;

//    private final IotService iotService;

    public Flux<GasConsumeLossRes> getAccumulate(){
        return queryHelper
            .select("SELECT \n" +
                        "CASE WHEN cur.difference > 0\n" +
                        "THEN cur.difference ELSE \"0\" \n" +
                        "END difference,\n" +
                        "cur.unit_price AS price\n" +
                        "FROM \n" +
                        "(SELECT \n" +
                        "SUM(difference) AS difference,\n" +
                        "ROUND(SUM(difference * unit_price ),2)  AS unit_price \n" +
                        "FROM \n" +
                        "sems_gas_consume\n" +
                        "WHERE device_id = '0' AND\n" +
                        "FROM_UNIXTIME( ROUND( gather_time / 1000, 0 ), \"%Y\" ) = YEAR(CURRENT_DATE) -- 当前年份\n" +
                        ") cur", GasConsumeLossRes::new)
            .fetch();
    }

}
