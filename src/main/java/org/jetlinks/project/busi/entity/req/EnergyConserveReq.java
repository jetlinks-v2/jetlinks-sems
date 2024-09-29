package org.jetlinks.project.busi.entity.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class EnergyConserveReq {

    @Schema(description = "节能前时间段")
    private Long[] conserveStartTime;

    @Schema(description = "节能后时间段")
    private Long[] conserveEndTime;
}
