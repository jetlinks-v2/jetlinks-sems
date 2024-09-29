package org.jetlinks.project.busi.entity.res;


import lombok.Data;

import java.math.BigDecimal;


@Data
public class EnergyCountRes {

    /**时间*/
    private String gatherTime;

    /**数量*/
    private BigDecimal number;

    /**区域id*/
    private String areaId;
    /**区域名称*/
    private String areaName;

    /**试验id*/
    private String configId;
    /**试验名称*/
    private String testName;

}
