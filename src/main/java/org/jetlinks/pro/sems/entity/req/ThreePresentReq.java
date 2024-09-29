package org.jetlinks.pro.sems.entity.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class ThreePresentReq {

    @Schema(description = "告警编码")
    private String code;

    @Schema(description = "应用系统")
    private String app;

    @Schema(description = "告警日期")
    private String alarmTime;

    @Schema(description = "告警类型")
    private String alarmType;

    @Schema(description = "告警名称")
    private String alarmName;

    @Schema(description = "告警内容")
    private String alarmContent;

    @Schema(description = "处理人员")
    private String solvePeople;

    @Schema(description = "处理日期")
    private String solveTime;

    @Schema(description = "操作")
    private String operation;

}
