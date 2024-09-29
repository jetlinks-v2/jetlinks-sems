package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;

import java.math.BigDecimal;

@Data
public class ElectricityConsumeLossRes {

    @Schema(description = "耗损")
    private BigDecimal difference;

    @Schema(description = "上报时间")
    private String gatherTime;

    @Schema(description = "金额")
    @DefaultValue("0")
    private BigDecimal price;
}
