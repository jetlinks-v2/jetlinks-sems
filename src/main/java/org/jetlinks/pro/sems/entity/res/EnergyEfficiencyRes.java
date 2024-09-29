package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EnergyEfficiencyRes {


    @Schema(description = "能耗")
    private BigDecimal energy;

    @Schema(description = "费用")
    private BigDecimal cost;


}
