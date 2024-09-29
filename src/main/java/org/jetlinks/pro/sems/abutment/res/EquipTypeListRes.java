package org.jetlinks.pro.sems.abutment.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class EquipTypeListRes {
    @Schema(description = "设备类型id")
    private String typeId;

    @Schema(description = "设备类型名称")
    private String typeName;
}
