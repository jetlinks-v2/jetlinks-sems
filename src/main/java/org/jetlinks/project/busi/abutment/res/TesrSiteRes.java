package org.jetlinks.project.busi.abutment.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TesrSiteRes {
    @Schema(description = "场所主键id")
    private String id;

    @Schema(description = "场所名称")
    private String areaName;

    @Schema(description = "场所状态 1启用0禁用")
    private String areaActive;

    @Schema(description = "数据状态")
    private String status;
}
