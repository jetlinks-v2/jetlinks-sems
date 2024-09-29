package org.jetlinks.project.busi.entity.res;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class BoardReportRes {
    private String time;

    private BigDecimal totalUse;

    private BigDecimal totalCost;

    private BigDecimal dayAvgUse;

    private BigDecimal monthAvgUse;

    private BigDecimal carBon;

    private BigDecimal coal;

    private BigDecimal _1user;

    private BigDecimal _1cost;

    private BigDecimal _2user;

    private BigDecimal _2cost;

    private BigDecimal _3user;

    private BigDecimal _3cost;

    private BigDecimal _4user;

    private BigDecimal _4cost;

}
