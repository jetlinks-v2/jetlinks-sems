package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DeviceMonitorStatusRes {

    @Schema(description = "频率")
    private BigDecimal frequency;

    @Schema(description = "总功率因数")
    private BigDecimal powerFactorTotal;

    @Schema(description = "三相电压不平衡度")
    private BigDecimal threeVoltage = BigDecimal.ZERO;

    @Schema(description = "三相电流不平衡度")
    private BigDecimal threePhase = BigDecimal.ZERO;

    @Schema(description = "年度总耗电量")
    private List<EnergyDayRes> yearEnergyList;

    @Schema(description = "A相电流")
    private EnergyConserveRes phaseIA;

    @Schema(description = "B相电流")
    private EnergyConserveRes phaseIB;

    @Schema(description = "C相电流")
    private EnergyConserveRes phaseIC;

    @Schema(description = "A相电压")
    private EnergyConserveRes phaseUA;

    @Schema(description = "B相电压")
    private EnergyConserveRes phaseUB;

    @Schema(description = "C相电压")
    private EnergyConserveRes phaseUC;

    @Schema(description = "A相有功功率")
    private EnergyConserveRes activePA;

    @Schema(description = "B相有功功率")
    private EnergyConserveRes activePB;

    @Schema(description = "C相有功功率")
    private EnergyConserveRes activePC;

    @Schema(description = "总有功功率")
    private EnergyConserveRes power;

    @Schema(description = "A相无功功率")
    private EnergyConserveRes reactivePA;

    @Schema(description = "B相无功功率")
    private EnergyConserveRes reactivePB;

    @Schema(description = "C相无功功率")
    private EnergyConserveRes reactivePC;

    @Schema(description = "总无功功率")
    private EnergyConserveRes reactivePTotal;

}
