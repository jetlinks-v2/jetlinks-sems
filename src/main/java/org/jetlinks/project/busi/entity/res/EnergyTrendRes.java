package org.jetlinks.project.busi.entity.res;

import lombok.Data;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;

import java.math.BigDecimal;

@Data
public class EnergyTrendRes {

    private String time;

    @DefaultValue("0")
    private BigDecimal value;

    private BigDecimal cost;

    private String type;
}
