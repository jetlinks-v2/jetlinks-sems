package org.jetlinks.project.busi.abutment.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class VXSendReq {

    @Schema(description = "系统标识")
    private String appSn;

    @Schema(description = "接收者   多个用,逗号分隔开")
    private String receiver;

    @Schema(description = "消息模板 Id")
    private Integer messageTemplateId;

    @Schema(description = "消息发送渠道 30-短信 40-邮件 70-企业微信")
    private Integer sendChannel;

    @Schema(description = "发送的内容")
    private String msgContent;

    @Schema(description = "消息类型 10- 通知类消息 20-营销类消息 30-验证码类消息")
    private Integer msgType;

    @Schema(description = "模板标题")
    private String name;
}
