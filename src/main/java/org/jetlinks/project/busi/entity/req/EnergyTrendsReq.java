package org.jetlinks.project.busi.entity.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class EnergyTrendsReq {

    private String type;

    /**统计维度，2月，3季*/
    private Integer dimension;

    /**开始时间*/
    private Date startDate;
    /**结束时间*/
    private Date endDate;


    private String year;
}
