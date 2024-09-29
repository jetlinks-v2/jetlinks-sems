package org.jetlinks.project.busi.entity.dto;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.0
 */
@Data
@ToString
public class HistoryDataDto {

    /**上报时间*/
    private Long gatherTime;

    /**差值*/
    private BigDecimal difference;

    /**尖峰平谷标识*/
    private Integer periodsType;
}
