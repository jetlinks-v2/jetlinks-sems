package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeviceMonitorInfoRes {

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "开机小时数")
    private BigDecimal powerOnHour = BigDecimal.ZERO;

    @Schema(description = "运行小时数")
    private BigDecimal runHour = BigDecimal.ZERO;

    @Schema(description = "开机率")
    private BigDecimal powerOnRate = BigDecimal.ZERO;

    @Schema(description = "总耗电量")
    private BigDecimal energySum = BigDecimal.ZERO;

}
