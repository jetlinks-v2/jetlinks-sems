package org.jetlinks.pro.sems.entity.req;

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
public class HomePageReq {

    @Schema(description = "查询种类")
    String type;
    @Schema(description = "统计维度:1日 2周 3月 4年")
    Integer dateType;
}
