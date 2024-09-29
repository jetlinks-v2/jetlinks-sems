package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DevicePieRes {

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "所占比例")
    private BigDecimal rate;

    @Schema(description = "能耗量")
    private BigDecimal difference;
}
