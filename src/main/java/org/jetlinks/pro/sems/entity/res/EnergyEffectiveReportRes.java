package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EnergyEffectiveReportRes {

    @Schema(description = "时间")
    private String time;

    @Schema(description = "有效能耗")
    private BigDecimal effectiveEnergy;

    @Schema(description = "有效费用")
    private BigDecimal effectiveCost;

    @Schema(description = "无效能耗")
    private BigDecimal invalidEnergy;

    @Schema(description = "无效费用")
    private BigDecimal invalidCost;
}
