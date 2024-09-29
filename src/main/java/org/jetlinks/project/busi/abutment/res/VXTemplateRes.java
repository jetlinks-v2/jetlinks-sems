package org.jetlinks.project.busi.abutment.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class VXTemplateRes {

    @Schema(description = "模板id")
    private Integer id;

    @Schema(description = "消息模板内容")
    private String msgContent;

    @Schema(description = "消息标题")
    private String name;

    @Schema(description = "消息发送渠道 30-短信 40-邮\n" +
        "件 70-企业微信")
    private Integer sendChannel;

    @Schema(description = "消息类型 10-通知类消息 20- 营销类消息 30-验证码类消\n" +
        "息")
    private Integer msgType;

    @Schema(description = "系统标识")
    private String appSn;
}
