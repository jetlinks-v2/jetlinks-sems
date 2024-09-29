package org.jetlinks.project.busi.entity.req;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetlinks.project.busi.enums.EnergyType;

import java.util.Date;

@Data
public class EnergyStructureReq {
    /**区域名称*/
    @Schema(description = "区域名称", example = "重庆南岸区")
    private String areaName;
    /**能源类型：1水，2电，3气*/
    @Schema(description = "能源类型")
    private EnergyType energyType;
    /**开始时间*/
    @Schema(description = "查询开始时间")
    private long startDate;
    /**结束时间*/
    @Schema(description = "查询结束时间")
    private long endDate;
}
