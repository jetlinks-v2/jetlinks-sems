package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TestEnergyAndCostRes {

    @Schema(description = "时间轴坐标")
    private String time;

    @Schema(description = "时间轴坐标对应的开始时间")
    private Long timeStart;

    @Schema(description = "时间轴坐标对应的结束时间")
    private Long timeEnd;

    @Schema(description = "条目名称")
    private String itemName;

    @Schema(description = "能耗")
    private BigDecimal number;

    @Schema(description = "费用")
    private BigDecimal cost;

    @Schema(description = "试验次数")
    private int num;

    @Schema(description = "查询时间段内试验开始时间")
    private Long testStartTime;

    @Schema(description = "查询时间段内试验结束时间")
    private Long testEndTime;

}
