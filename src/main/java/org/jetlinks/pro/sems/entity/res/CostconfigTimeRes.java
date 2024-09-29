package org.jetlinks.pro.sems.entity.res;

import lombok.Data;

@Data
public class CostconfigTimeRes {

    /**开始时间*/
    private String startDate;

    /**结束时间*/
    private String endDate;

    /**排序*/
    private Integer sort;
}
