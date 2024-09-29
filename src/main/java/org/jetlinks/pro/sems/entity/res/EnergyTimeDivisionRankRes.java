package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**分时能耗排名返回参数
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.0
 */
@Data
@NoArgsConstructor
@ToString
public class EnergyTimeDivisionRankRes {
    /*能耗排名*/
    /*区域名称*/
    @Schema(description = "区域名称")
    private String areaName;
    /*区域名称*/
    @Schema(description = "区域id")
    private String areaId;
    /*区域名称*/
    @Schema(description = "试验名称")
    private String testName;
    /*总能耗*/
    @Schema(description = "总能耗")
    private BigDecimal totalEnergyConsumption;
    /*总费用*/
    @Schema(description = "总费用")
    private BigDecimal totalExpense;

    public EnergyTimeDivisionRankRes(String testName,BigDecimal totalEnergyConsumption, BigDecimal totalExpense) {
        this.totalEnergyConsumption = totalEnergyConsumption;
        this.totalExpense = totalExpense;
    }
}
