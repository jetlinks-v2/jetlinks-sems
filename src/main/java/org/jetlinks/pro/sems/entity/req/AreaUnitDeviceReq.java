package org.jetlinks.pro.sems.entity.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AreaUnitDeviceReq {
    @Schema(description = "区域id")
    private String areaId;

    @Schema(description = "设备id组")
    private String[] deviceIds;
}
