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
public class EnergyForecastTrendInfo {
    /*时间*/
    @Schema(description = "时间")
    private String date;
    /*时间*/
    @Schema(description = "当前")
    private BigDecimal number;
}
