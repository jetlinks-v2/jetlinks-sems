package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EnergyNewBenefitsRes {
    @Schema(description = "区域")
    private String region;

    @Schema(description = "时段1的能耗")
    private BigDecimal costOne;

    @Schema(description = "时段2的能耗")
    private BigDecimal costTwo;
}
