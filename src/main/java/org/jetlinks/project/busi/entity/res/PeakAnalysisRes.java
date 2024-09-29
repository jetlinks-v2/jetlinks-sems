package org.jetlinks.project.busi.entity.res;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName PeakAnalysisRes
 * @Author hky
 * @Time 2023/7/12 16:54
 * @Description
 **/
@Getter
@Setter
public class PeakAnalysisRes {


    private BigDecimal peakValue;

    private BigDecimal troughValue;

    private List<PeakAnalysisInfo> thread;


    public PeakAnalysisRes(HashMap<Long, BigDecimal> map) {
        Optional
            .ofNullable(map)
            .map(map1 -> map.values())
            .ifPresent(values -> {
                if (values.size() >0){
                    peakValue = Collections.max(values);
                    troughValue = Collections.min(values);
                    thread =  map.entrySet()
                                 .stream()
                                 .map(PeakAnalysisInfo::new)
                                 .collect(Collectors.toList());
                }

            });
    }

}
