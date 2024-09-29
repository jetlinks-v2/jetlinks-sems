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
public class EnergyForecastCurMonthRes {
    /*本月用量*/
    @Schema(description = "本月用量")
    private BigDecimal curMonthNumber;
    /*本月目前用量月同比*/
    @Schema(description = "本月目前用量月同比")
    private BigDecimal curMonthOnMonth;
    /*本月预测用量*/
    @Schema(description = "本月预测用量")
    private BigDecimal forMonthNumber;
    /*预测用量月同比*/
    @Schema(description = "预测用量月同比")
    private BigDecimal forMonthOnMonth;
    /*本月预测用量*/
    @Schema(description = "本月预测费用")
    private BigDecimal forMonthExpense;
    /*预测费用月同比*/
    @Schema(description = "预测费用月同比")
    private BigDecimal forMonthOnMonthExpense;
}
