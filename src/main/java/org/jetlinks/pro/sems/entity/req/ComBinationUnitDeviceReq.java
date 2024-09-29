package org.jetlinks.pro.sems.entity.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetlinks.pro.sems.enums.EnergyType;

import java.math.BigDecimal;

@Data
public class ComBinationUnitDeviceReq {

    @Schema(description = "组合设备ID")
    private String id;

    @Schema(description = "组合设备名称")
    private String unitName;

    @Schema(description = "设备ID数组")
    private String[] deviceId;

    @Schema(description = "能源类型")
    private EnergyType[] energyType;

    @Schema(description = "查询开始时间")
    private Long beginTime;

    @Schema(description = "查询结束时间")
    private Long endTime;

    private BigDecimal count;
}
