package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TestAreaAnalysisRes {
    @Schema(description = "试验名称")
    private String areaName;

    @Schema(description = "能耗")
    private BigDecimal energy;

    @Schema(description = "同比")
    private String yoy;

    @Schema(description = "环比")
    private String qoq;
}
