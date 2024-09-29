package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeviceCurrentInfoRes {

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "总有功功率")
    private BigDecimal power;

    @Schema(description = "设备状态 0-未知状态 1-开机 2-关机 3-运行 4-停止 5-报警 6-故障")
    private String computeStatus;

    @Schema(description = "今日能耗")
    private BigDecimal difference;

    @Schema(description = "今日作业时长")
    private BigDecimal runTime = BigDecimal.ZERO;

    @Schema(description = "今日开机时长")
    private Long onTime;
}
