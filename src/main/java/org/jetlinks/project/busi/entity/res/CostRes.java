package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @ClassName CostRes
 * @Author hky
 * @Time 2023/7/17 11:37
 * @Description 成本分析返回
 **/
@Data
@Builder
public class CostRes {


    @Schema(description = "区域")
    private String region;

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
    private BigDecimal cost;

    @Schema(description = "标志")
    private Integer flag;
}
