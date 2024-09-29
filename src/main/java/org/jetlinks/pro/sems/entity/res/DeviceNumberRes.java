package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeviceNumberRes {

    @Schema(description = "设备总数")
    private Integer deviceTotal = 0;

    @Schema(description = "开机总数")
    private Integer powerOnNumber = 0;

    @Schema(description = "关机总数")
    private Integer shutdownNumber = 0;

    @Schema(description = "运行")
    private Integer runNumber = 0;

    @Schema(description = "停止")
    private Integer stopNumber = 0;

    @Schema(description = "设备年度总耗电量")
    private BigDecimal energySum = BigDecimal.ZERO;
}
