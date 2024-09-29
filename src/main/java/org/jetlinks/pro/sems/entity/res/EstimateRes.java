package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetlinks.pro.cluster.ClusterEventBus;

@Data
public class EstimateRes {

    @Schema(description = "条目id")
    private String itemId;

    @Schema(description = "条目名称")
    private String itemName;

    @Schema(description = "预估时间")
    private String time;

    @Schema(description = "预估值")
    private String estimateNum;
}
