package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WaterConsumeLossRes {

    @Schema(description = "耗损")
    private BigDecimal difference;

    @Schema(description = "上报时间")
    private String gatherTime;

    @Schema(description = "金额")
    private BigDecimal price;
}
