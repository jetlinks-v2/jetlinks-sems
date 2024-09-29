package org.jetlinks.project.busi.abutment.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MessageContentReq {
    @Schema(description = "系统标识")
    private String appSn;

    @Schema(description = "消息类型 1- 通知 2-告警")
    private String messageType;

    @Schema(description = "消息类容")
    private String msgContent;

    @Schema(description = "消息标题")
    private String msgTitle;

    @Schema(description = "是否需要操作")
    private Boolean needOperate;

    @Schema(description = "处理链接")
    private String operateUrl;

    @Schema(description = "消息流水号,子系统唯一用以判断是否重复消息")
    private String outOrderNo;

    @Schema(description = "接收者用户id")
    private String  receiverId;

    @Schema(description = "设备id")
    private String equipmentId;

    @Schema(description = "设备名称")
    private String equipmentName;

    @Schema(description = "告警类型")
    private String alarmType;
}
