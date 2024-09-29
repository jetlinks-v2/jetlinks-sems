package org.jetlinks.project.busi.abutment.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UpdateTestRecordRes {
    @Schema(description = "项目id",nullable = true)
    private String projId;

    @Schema(description = "条目id",nullable = true)
    private String experimentItemId;

    @Schema(description = "条目结束时间（到分钟如2023-" +
        "01-01 12:21） 条目结束的时候能否把开始时间也传过来")
    private String experimentEndTime;

    @Schema(description = " 条目开始时间（到分钟 如 2023-" +
        "01-01 12:21）")
    private String experimentStartTime;


}
