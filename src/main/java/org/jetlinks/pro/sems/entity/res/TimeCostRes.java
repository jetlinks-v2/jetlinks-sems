package org.jetlinks.pro.sems.entity.res;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TimeCostRes {
    /**时间*/
    private String gatherTime;

    /**数量*/
    private BigDecimal difference;

    /**区域id*/
    private String areaId;
    /**区域名称*/
    private String areaName;

    /**试验id*/
    private String configId;
    /**试验名称*/
    private String testName;

    private BigDecimal unitPrice;

    private BigDecimal cost;

    private String periodsType;
}
