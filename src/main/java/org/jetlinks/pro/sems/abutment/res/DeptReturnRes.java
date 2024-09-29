package org.jetlinks.pro.sems.abutment.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DeptReturnRes {

    @Schema(description = "部门id")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "上级部门id")
    private Long parentId;

    @Schema(description = "用户状态 0 正常 1 禁用")
    private Integer status;

    @Schema(description = "部门负责人")
    private String leader;
}
