package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetlinks.project.busi.enums.EnergyType;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "综合报表实体类")
public class SyntheticReportRes {

    @Schema(description = "能源类型")
    private EnergyType energyType;

    @Schema(description = "所属区域")
    private String areaId;

    @Schema(description = "区域名")
    private String areaName;

    @Schema(description = "能耗值")
    private BigDecimal number;

    //能耗列表
    private List<DataConsumeRes> list;

}
