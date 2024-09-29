package org.jetlinks.pro.sems.entity.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetlinks.pro.sems.enums.EnergyType;

@Data
public class EnergyReportReq {

    @Schema(description = "能源类型")
    private EnergyType energyType;

    @Schema(description = "区域path")
    private String path;

    @Schema(description = "查询时间区间")
    private Long[] gatherTime;

    private Integer pageIndex;

    private Integer pageSize;


}
