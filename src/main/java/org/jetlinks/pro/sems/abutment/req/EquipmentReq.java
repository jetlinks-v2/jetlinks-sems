package org.jetlinks.pro.sems.abutment.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class EquipmentReq {

    @Schema(description = "设备类型")
    private String eqType;

    @Schema(description = "资产状态")
    private String assetState;

    @Schema(description = "设备名称")
    private String equipmentName;

    @Schema(description = "父级设备 ID")
    private String parentEquipId;

    @Schema(description = "物联设备")
    private Boolean iot;

    @Schema(description = "设备负责人")
    private String head;

    @Schema(description = "设备id")
    private String equipmentId;

    @Schema(description = "设备编码")
    private String equipmentCode;

    @Schema(description = "删除标记 0：删除，1：未删除")
    private Integer enable;

    private int pageIndex;

    private int pageSize;

    private List<String> ids;
}
