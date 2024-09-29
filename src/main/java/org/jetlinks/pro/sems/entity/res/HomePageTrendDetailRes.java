package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

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
public class HomePageTrendDetailRes {

    @Schema(description = "时间")
    String time;
    @Schema(description = "当前能耗")
    BigDecimal nowEnergyConsume;
    @Schema(description = "去年能耗")
    BigDecimal lastEnergyConsume;
}
