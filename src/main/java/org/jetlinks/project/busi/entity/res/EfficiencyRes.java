package org.jetlinks.project.busi.entity.res;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EfficiencyRes {

    private BigDecimal totalEnergy;

    private BigDecimal totalCost;
}
