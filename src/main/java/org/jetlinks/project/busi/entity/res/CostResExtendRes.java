package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CostResExtendRes {
    @Schema(description = "区域")
    private String deviceName;

    @Schema(description = "试验")
    private String testName;

    @Schema(description = "尖时时段")
    private BigDecimal cuspPeriods;

    @Schema(description = "峰时时段")
    private BigDecimal peakPeriods;

    @Schema(description = "平时时段")
    private BigDecimal flatPeriods;

    @Schema(description = "谷时时段")
    private BigDecimal valleyPeriods;

    @Schema(description = "费用")
    private BigDecimal unitPrice;

    @Schema(description = "标志")
    private Integer periodsType;

    @Schema(description = "用量")
    private BigDecimal difference;
}
