package org.jetlinks.pro.sems.entity.res;

import lombok.Data;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;

import java.math.BigDecimal;

/**
 * 能耗看板报告字段
 */
@Data
public class BoardPicReportRes {

    private String time;

    private BigDecimal totalUse;

    @DefaultValue("0")
    private  BigDecimal historyUse;

    @DefaultValue("0")
    private BigDecimal yoy;

    @DefaultValue("0")
    private BigDecimal lastUse;

    @DefaultValue("0")
    private BigDecimal qoq;

    @DefaultValue("")
    private String time2;

    @DefaultValue("")
    private String tag;

    @DefaultValue("0")
    private BigDecimal rat;

    @DefaultValue("")
    private String tag1;

    @DefaultValue("0")
    private BigDecimal rat1;
}
