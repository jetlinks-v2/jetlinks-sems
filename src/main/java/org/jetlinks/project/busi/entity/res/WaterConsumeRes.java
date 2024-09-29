package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetlinks.project.busi.entity.WaterConsumeEntity;

import java.math.BigDecimal;

@Data
public class WaterConsumeRes extends WaterConsumeEntity {

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "上次表数")
    private BigDecimal beginNumber;
}
