package org.jetlinks.pro.sems.strategy.peak;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.pro.sems.entity.res.CostRes;
import org.jetlinks.pro.sems.entity.res.PeakAnalysisRes;
import org.jetlinks.pro.sems.entity.res.PeakUniversalData;
import org.jetlinks.pro.sems.enums.TimeEnum;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @ClassName PeakAnalysisStrategy
 * @Author hky
 * @Time 2023/7/12 16:45
 * @Description 水电气
 **/
public interface PeakAnalysisStrategy {


    /**
     * 查询时间段
     * @param query
     * @param timeEnum
     * @return
     */
     Mono<PeakAnalysisRes> getPeakDetail(QueryParamEntity query, TimeEnum timeEnum);


     Flux<PeakUniversalData> getPeakData(QueryParamEntity query);

    Mono<List<CostRes>> getRegionPeakData(QueryParamEntity query);

     Mono<Map<String,Object>> getRegionYearAnalysis(QueryParamEntity queryParam);

    Mono<Map<String,Object>> getTestYearAnalysis(QueryParamEntity queryParam);

    Flux<CostRes> getTestElectricCost(QueryParamEntity query);

    Flux<CostRes> getRegionElectricCost(QueryParamEntity query);

   Flux<CostRes> getTestPeakData(QueryParamEntity query);







}
