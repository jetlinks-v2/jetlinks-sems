package org.jetlinks.project.busi.entity.res;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 成本分析报告
 */
@Data
public class CostAnalysisReport {

    private String time;

    private String areaName;

    private BigDecimal peakUse;

    private BigDecimal peakCost;

    private BigDecimal hightUse;

    private BigDecimal highCost;

    private BigDecimal flatUse;

    private BigDecimal flatCost;

    private BigDecimal lowUse;

    private BigDecimal lowCost;
}
