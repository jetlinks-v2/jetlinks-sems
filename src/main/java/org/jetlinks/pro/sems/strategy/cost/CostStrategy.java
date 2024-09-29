package org.jetlinks.pro.sems.strategy.cost;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.pro.sems.entity.res.CostRes;
import org.jetlinks.pro.sems.enums.EnergyType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @ClassName CostStrategy
 * @Author hky
 * @Time 2023/7/17 11:33
 * @Description 成本策略
 **/
public interface CostStrategy {


    /**
     * 获取成本占比
     * @param queryParamEntity 查询条件
     * @return
     */
    Flux<CostRes>  getCostProportion(QueryParamEntity queryParamEntity, EnergyType energyType);

    /**
     * 获取成本条目
     * @param queryParamEntity 查询条件
     * @return
     */

    Flux<CostRes> getCostEntry(QueryParamEntity queryParamEntity,EnergyType energyType);

    /**
     * 获取年度分析
     * @param queryParamEntity 查询条件
     * @return
     */
    Mono<Map<String, Object>> getThreadByYear(QueryParamEntity queryParamEntity, EnergyType energyType);

}
