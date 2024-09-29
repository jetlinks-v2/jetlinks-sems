package org.jetlinks.pro.sems.entity.res;

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
public class HomePageTrendInfo {
    @Schema(description = "日期")
    private String date;
    @Schema(description = "能耗")
    private BigDecimal number;
}
