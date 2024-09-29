package org.jetlinks.project.busi.entity.res;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class YoyAndqoqReturnRes {

    private Integer month;

    private BigDecimal thisYearEnergy;

    private BigDecimal lastYearEnergy;

    private BigDecimal lastMonthEnergy;

    private String yoy;

    private String qoq;
}
