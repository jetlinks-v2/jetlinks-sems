package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class EntryMonthRes {

    private String month;

    private BigDecimal cost;

    @Schema(description = "尖时时段")
    private Map cuspPeriods;

    @Schema(description = "峰时时段")
    private Map peakPeriods;

    @Schema(description = "平时时段")
    private Map flatPeriods;

    @Schema(description = "谷时时段")
    private Map valleyPeriods;

    private Integer type;

    private BigDecimal difference;
}
