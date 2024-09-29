package org.jetlinks.pro.sems.abutment.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UserReturnRes {

    @Schema(description = "用户id")
    private String userId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "昵称")
    private String nickName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "电话")
    private String phonenumber;

    @Schema(description = "部门id")
    private String deptId;

    @Schema(description = "状态，0正常，1禁用")
    private String status;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "企业微信userId")
    private String wxUserId;

    @Schema(description = "删除标志 0代表存在，1代表删除")
    private String delFlag;
}
