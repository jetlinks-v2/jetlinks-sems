package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TestAnalysisReportRes {

    @Schema(description = "时间")
    private String time;

    @Schema(description = "场所名称")
    private String areaName;

    @Schema(description = "能源占比")
    private String rate;

    @Schema(description = "场所总耗电量")
    private BigDecimal total;

    @Schema(description = "用能费用")
    private BigDecimal cost;

    @Schema(description = "")
    private String remarkArea;

    @Schema(description = "环比比率")
    private Number remark;
}
