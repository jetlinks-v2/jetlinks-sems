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
public class HomePageEnergyMonthInfo {
    @Schema(description = "月份")
    Integer month;
    @Schema(description = "能耗")
    BigDecimal energyConsume;
    @Schema(description = "费用")
    BigDecimal expense;
}
