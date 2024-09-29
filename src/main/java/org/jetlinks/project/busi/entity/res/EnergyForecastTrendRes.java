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
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "能耗预测趋势详细")
public class EnergyForecastTrendRes {
    /*时间*/
    @Schema(description = "时间")
    private String date;
    /*当前*/
    @Schema(description = "当前")
    private BigDecimal current;
    /*同比*/
    @Schema(description = "同比")
    private BigDecimal yearOnYear;
    /*同比率*/
    @Schema(description = "同比率")
    private BigDecimal yearOnYearRatio;
    /*环比*/
    @Schema(description = "环比")
    private BigDecimal monthOnMonth;
    /*环比率*/
    @Schema(description = "环比率")
    private BigDecimal monthOnMontRatio;
}
