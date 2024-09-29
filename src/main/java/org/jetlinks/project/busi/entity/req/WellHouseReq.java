package org.jetlinks.project.busi.entity.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetlinks.project.busi.entity.WellHouseMeterEntity;
import org.jetlinks.project.busi.enums.EnergyType;

import java.util.List;

@Data
public class WellHouseReq {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "位置")
    private String position;

    @Schema(description = "能源类型")
    private String energyType;

    @Schema(description = "井房表List")
    List<WellHouseMeterEntity> meterList;

}
