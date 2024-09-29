package org.jetlinks.project.busi.entity.res;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class EnergyStructureRes {
    /*月份*/
    private Integer month;
    /*yyyy-mm格式时间*/
    private String date;
    /*月度能耗*/
    private BigDecimal monthEnergyConsumption;
    /*环比*/
    private String qoq;
    /*同比*/
    private String yoy;
    /*标准煤*/
    private BigDecimal standardCoal;
    /*碳排放*/
    private BigDecimal carbonEmission;
    /*费用*/
    private BigDecimal expense;
}
