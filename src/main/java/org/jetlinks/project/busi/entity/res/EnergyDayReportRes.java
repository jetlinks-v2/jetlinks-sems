package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetlinks.project.busi.enums.EnergyType;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "能耗日清实体类")
public class EnergyDayReportRes {

    @Schema(description = "能源类型")
    private String energy;

    @Schema(description = "时间能耗集合")
    List<EnergyDayRes> energyDayResList;

    @Schema(description = "月平均能耗")
    private BigDecimal monthAvgEnergy;

    @Schema(description = "月总能耗")
    private BigDecimal monthSumEnergy;

    @Schema(description = "总费用")
    private BigDecimal totalCost;

    @Schema(description = "同比")
    private BigDecimal yoy;

    @Schema(description = "环比")
    private BigDecimal qoq;

}
