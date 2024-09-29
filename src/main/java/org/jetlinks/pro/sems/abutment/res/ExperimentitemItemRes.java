package org.jetlinks.pro.sems.abutment.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ExperimentitemItemRes {

    @Schema(description = "条目名称")
    private String itemName;

    @Schema(description = "条目编号")
    private String id;

    @Schema(description = "条目属性 1 AWT 2 CWT")
    private String itemAttribute;

    @Schema(description = "场所类型")
    private String experimentAreaTypeId;

    @Schema(description = "启用状态 1启用 0禁用")
    private String itemActive;

    @Schema(description = "数据状态 数据状态0未删除 1已删除")
    private String status;

    @Schema(description = "描述")
    private String remark;
}
