package org.jetlinks.project.busi.strategy.cost;

import lombok.RequiredArgsConstructor;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.project.busi.entity.TestConfigDeviceEntity;
import org.jetlinks.project.busi.entity.res.CostRes;
import org.jetlinks.project.busi.entity.res.EnergyCountRes;
import org.jetlinks.project.busi.entity.res.PeakUniversalData;
import org.jetlinks.project.busi.enums.EnergyType;
import org.jetlinks.project.busi.factory.PeakFactory;
import org.jetlinks.project.busi.service.DeviceService;
import org.jetlinks.project.busi.service.TestConfigDeviceService;
import org.jetlinks.project.busi.service.TestConfigService;
import org.jetlinks.project.busi.strategy.cost.handler.CostProportionHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName ExperimentStrategy
 * @Author hky
 * @Time 2023/7/17 13:43
 * @Description 实验策略
 **/
@Component("test")
@RequiredArgsConstructor
public class ExperimentStrategy extends AbstractCost{

    private final PeakFactory peakFactory;

    @Override
    public Flux<CostRes> getCostProportion(QueryParamEntity queryParamEntity, EnergyType energyType) {
        return peakFactory.getPeakStrategy(energyType.name())
//            .getPeakData(queryParamEntity)
//            .collectMultimap(PeakUniversalData::getDeviceId)
//            .flatMap(CostProportionHandler::reduceTotalValue)
//            .flatMapMany(map -> CostProportionHandler.buildTestData(map,testConfigDeviceService,testConfigService))
//            .sort(Comparator.comparing(CostRes::getCost).reversed())
            .getTestPeakData(queryParamEntity)
            .collectList()
            .flatMapMany(i->{
                if(i.size()>5){
                    List<CostRes> costResList = i.subList(5, i.size());
                    BigDecimal total = costResList.stream().map(CostRes::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);

                    List<CostRes> costRes = i.subList(0, 5);
                    costRes.add(CostRes.builder()
                        .region("其他")
                        .cost(total)
                        .build());
                    return Flux.fromIterable(costRes);
                }
                return Flux.fromIterable(i);
            });
    }

    @Override
    public Flux<CostRes> getCostEntry(QueryParamEntity queryParamEntity, EnergyType energyType) {
        if(energyType.equals(EnergyType.electricity)){
            return peakFactory.getPeakStrategy(energyType.name())
                .getTestElectricCost(queryParamEntity)
                .sort(Comparator.comparing(CostRes::getTestName));
        }else {
            return peakFactory.getPeakStrategy(energyType.name())
                .getTestPeakData(queryParamEntity)
            .sort(Comparator.comparing(CostRes::getTestName));
            //    .getPeakData(queryParamEntity);
//                .collectMultimap(PeakUniversalData::getDeviceId)
//                .flatMap(CostProportionHandler::reduceTotalValue)
//                .flatMapMany(map -> CostProportionHandler.buildTestData(map,testConfigDeviceService, testConfigService));
        }
    }

    @Override
    public Mono<Map<String, Object>> getThreadByYear(QueryParamEntity queryParamEntity, EnergyType energyType) {
        return peakFactory.getPeakStrategy(energyType.name())
            .getTestYearAnalysis(queryParamEntity);
    }
}
