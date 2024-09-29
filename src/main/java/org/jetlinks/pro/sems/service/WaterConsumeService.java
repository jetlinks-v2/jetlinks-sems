package org.jetlinks.pro.sems.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.pro.sems.entity.res.WaterConsumeLossRes;
import org.jetlinks.pro.sems.entity.WaterConsumeEntity;
//import org.jetlinks.project.busi.iot.IotService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@AllArgsConstructor
public class WaterConsumeService extends GenericReactiveCrudService<WaterConsumeEntity,String> {

    private final QueryHelper queryHelper;

    private final AreaInfoService areaInfoService;

//    private final IotService iotService;


    public Flux<WaterConsumeLossRes> getAccumulate(){
         return queryHelper
             .select("SELECT \n" +
                         "cur.difference difference,\n" +
                         "cur.unit_price AS price\n" +
                         "FROM \n" +
                         "(SELECT \n" +
                         "SUM(difference) AS difference,\n" +
                         "ROUND(SUM(difference * unit_price),2) AS unit_price \n" +
                         "FROM \n" +
                         "sems_water_consume\n" +
                         "WHERE device_id = '0' AND \n" +
                         "FROM_UNIXTIME( ROUND( gather_time / 1000, 0 ), '%Y' ) = YEAR(CURRENT_DATE) \n" +
                         ") cur", WaterConsumeLossRes::new)
             .fetch();
    }
}
