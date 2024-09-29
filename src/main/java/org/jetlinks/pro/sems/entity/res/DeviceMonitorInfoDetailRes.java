package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeviceMonitorInfoDetailRes {

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "频率")
    private BigDecimal frequency;

    @Schema(description = "总功率因数")
    private BigDecimal powerFactorTotal;

    @Schema(description = "三相电压不平衡度")
    private BigDecimal threeVoltage;

    @Schema(description = "三相电流不平衡度")
    private BigDecimal threePhase;

}
