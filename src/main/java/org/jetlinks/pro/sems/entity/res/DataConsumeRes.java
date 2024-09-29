package org.jetlinks.pro.sems.entity.res;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DataConsumeRes {

    private Long gatherTime;

    private BigDecimal difference;

}
