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
@AllArgsConstructor
@NoArgsConstructor
public class HomePageEnergyMonthRes {
    @Schema(description = "月份")
    Integer month;
    @Schema(description = "本月能耗")
    BigDecimal nowEnergyConsume;
    @Schema(description = "去年同月能耗")
    BigDecimal lastEnergyConsume;
    @Schema(description = "同比")
    BigDecimal yearOverYearRatio;
    @Schema(description = "上月能耗")
    BigDecimal lastMonthEnergyConsume;
    @Schema(description = "环比")
    BigDecimal monthOverMonthRatio;
    @Schema(description = "费用")
    BigDecimal expense;

    public HomePageEnergyMonthRes(Integer month) {
        this.month = month;
        this.nowEnergyConsume = BigDecimal.ZERO;
        this.lastEnergyConsume = BigDecimal.ZERO;
        this.yearOverYearRatio = BigDecimal.ZERO;
        this.lastMonthEnergyConsume = BigDecimal.ZERO;
        this.monthOverMonthRatio = BigDecimal.ZERO;
        this.expense = BigDecimal.ZERO;
    }
}
