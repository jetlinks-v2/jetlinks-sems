package org.jetlinks.pro.sems.entity.res;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EnergyChartRes {
    private String name;

    private String rate;

    private BigDecimal num;

    private BigDecimal cost;
}
