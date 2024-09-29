package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.0
 */
@Data
public class HomePageRankRes {
    @Schema(description = "时间")
    private String date;
    @Schema(description = "能耗")
    private BigDecimal totalEnergyConsumption;
    @Schema(description = "类型")
    private String type;
}
