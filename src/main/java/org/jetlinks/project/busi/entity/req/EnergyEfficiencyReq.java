package org.jetlinks.project.busi.entity.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetlinks.pro.cluster.ClusterEventBus;

@Data
public class EnergyEfficiencyReq {

    @Schema(description = "搜索开始时间搓，精确到时分秒")
    private Long startDate;

    @Schema(description = "搜索结束时间搓，精确到时分秒")
    private Long endDate;
}
