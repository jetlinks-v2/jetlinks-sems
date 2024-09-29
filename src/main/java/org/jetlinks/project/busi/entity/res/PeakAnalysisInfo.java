package org.jetlinks.project.busi.entity.res;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @ClassName PeakAnalyInfo
 * @Author hky
 * @Time 2023/7/13 13:37
 * @Description
 **/
@Getter
@Setter
@Builder
public class PeakAnalysisInfo {

    private Long time;

    private BigDecimal value;

    public PeakAnalysisInfo() {
    }

    public PeakAnalysisInfo(Long time, BigDecimal value) {
        this.time = time;
        this.value = value;
    }

    public  PeakAnalysisInfo(Map.Entry<Long,BigDecimal> entry){
        time = entry.getKey();
        value = entry.getValue();
    }
}
