package org.jetlinks.project.busi.entity.dto;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class EnergyStructureDto {

    /**上报时间*/
    private Long gatherTime;

    /**差值*/
    private BigDecimal difference;

    /**价格*/
    private BigDecimal unitPrice;
}
