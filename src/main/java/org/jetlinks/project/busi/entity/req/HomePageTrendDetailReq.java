package org.jetlinks.project.busi.entity.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.0
 */
@Data
public class HomePageTrendDetailReq {
    @Schema(description = "查询种类")
    private String type;
    @Schema(description = "开始时间")
    private Long startTime;
    @Schema(description = "结束时间")
    private Long endTime;
}
