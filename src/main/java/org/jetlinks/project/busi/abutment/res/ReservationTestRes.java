package org.jetlinks.project.busi.abutment.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ReservationTestRes {

    @Schema(description = "试验预约表主键")
    private String id;

    @Schema(description = "预约试验开始时间")
    private String experimentStartTime;

    @Schema(description = "预约试验结束时间")
    private String experimentEndTime;

    @Schema(description = "取消状态 0未取消 1已取消")
    private String cancelStatus;

    @Schema(description = "数据状态 0未删除 1已删除")
    private String status;
}
