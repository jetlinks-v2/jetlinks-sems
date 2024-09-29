package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "时间能耗实体类")
public class EnergyDayRes {

    @Schema(description = "上报时间")
    private String gatherTime;

    @Schema(description = "能耗")
    private BigDecimal difference;

    @Schema(description = "费用")
    private BigDecimal cost;

}
