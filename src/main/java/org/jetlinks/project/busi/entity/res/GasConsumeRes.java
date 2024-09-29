package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetlinks.project.busi.entity.GasConsumeEntity;

import java.math.BigDecimal;

@Data
public class GasConsumeRes extends GasConsumeEntity {
    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "上次表数")
    private BigDecimal beginNumber;

    @Schema(description = "耗损")
    private BigDecimal difference;

    @Schema(description = "上报时间")
    private String reportTime;
}
