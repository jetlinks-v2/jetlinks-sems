package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomePageRealTimeRes {
    @Schema(description = "电能耗")
    private BigDecimal electricityEnergyConsume;
    @Schema(description = "电同比")
    private BigDecimal electricityPeriodOnPeriod;
    @Schema(description = "水能耗")
    private BigDecimal waterEnergyConsume;
    @Schema(description = "水同比")
    private BigDecimal waterPeriodOnPeriod;
    @Schema(description = "气能耗")
    private BigDecimal gasEnergyConsume;
    @Schema(description = "气同比")
    private BigDecimal gasPeriodOnPeriod;

}
