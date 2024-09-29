package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CostReturnRes {

    @Schema(description = "时间")
    private String time;

    @Schema(description = "场所名称")
    private String areaName;

    @Schema(description = "用量")
    private String number;
}
