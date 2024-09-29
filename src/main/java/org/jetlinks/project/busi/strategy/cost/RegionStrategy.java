package org.jetlinks.project.busi.strategy.cost;

import lombok.RequiredArgsConstructor;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.project.busi.entity.res.CostRes;
import org.jetlinks.project.busi.enums.EnergyType;
import org.jetlinks.project.busi.factory.PeakFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.CollationKey;
import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @ClassName RegionStrategy
 * @Author hky
 * @Time 2023/7/17 13:42
 * @Description 区域策略
 **/
@Component("region")
@RequiredArgsConstructor
public class RegionStrategy extends AbstractCost {

    private final PeakFactory peakFactory;


    @Override
    public Flux<CostRes> getCostProportion(QueryParamEntity queryParamEntity, EnergyType energyType) {
       return peakFactory.getPeakStrategy(energyType.name())
                   .getRegionPeakData(queryParamEntity)
           .flatMapMany(many->{
               List<CostRes> i = many.stream().sorted(Comparator.comparing(CostRes::getCost).reversed()).filter(value -> value.getRegion() != null).collect(Collectors.toList());

               if(i.size()>5){
                   List<CostRes> costRes = i.subList(5, i.size());
                   BigDecimal total = costRes.stream().map(CostRes::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);

                   List<CostRes> costRes1 = i.subList(0, 5);
                   costRes1.add(CostRes.builder()
                       .region("其他")
                       .cost(total)
                       .build());
                   return Flux.fromIterable(costRes1);
               }
               return Flux.fromIterable(i);
           });


    }


    @Override
    public Flux<CostRes> getCostEntry(QueryParamEntity queryParamEntity, EnergyType energyType) {
            return peakFactory.getPeakStrategy(energyType.name())
                .getRegionElectricCost(queryParamEntity)
                .collectList()
                .flatMapMany(list->{
                    List<CostRes> collect = list.stream().sorted(Comparator.comparing(CostRes::getRegion)).collect(Collectors.toList());
                    return Flux.fromIterable(collect);
            });


    }

    @Override
    public Mono<Map<String, Object>> getThreadByYear(QueryParamEntity queryParamEntity, EnergyType energyType) {
        return peakFactory.getPeakStrategy(energyType.name())
            .getRegionYearAnalysis(queryParamEntity);

    }
}
