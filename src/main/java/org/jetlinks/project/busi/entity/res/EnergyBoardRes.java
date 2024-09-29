package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EnergyBoardRes {

    @Schema(description = "日用量")
    private BigDecimal dailyUsage;

    @Schema(description = "较上日")
    private BigDecimal comparedPreviousDay;

    @Schema(description = "月用量")
    private BigDecimal monthlyUsage;

    @Schema(description = "较上月")
    private BigDecimal comparedLastMonth;

    @Schema(description = "季用量")
    private BigDecimal seasonalUsage;

    @Schema(description = "较上季")
    private BigDecimal comparedPreviousSeason;

    @Schema(description = "环比昨日")
    private String comparedToYesterday;

    @Schema(description = "环比上月")
    private String monthOnMonth;

    @Schema(description = "同比去年同季")
    private String yoy;

    @Schema(description = "环比上季")
    private String comparedToSeason;
}
