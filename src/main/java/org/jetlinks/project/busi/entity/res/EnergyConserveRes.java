package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class EnergyConserveRes {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "能耗列表")
    List<EnergyDayRes> energyList;

    @Schema(description = "总能耗")
    private BigDecimal sumEnergy;

}
