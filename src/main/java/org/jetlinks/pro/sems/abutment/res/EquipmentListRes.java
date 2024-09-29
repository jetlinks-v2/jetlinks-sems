package org.jetlinks.pro.sems.abutment.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class EquipmentListRes {
    @Schema(description = "设备 ID")
    private String equipmentId;

    @Schema(description = "设备编码")
    private String equipmentCode;

    @Schema(description = "设备名")
    private String equipmentName;

    @Schema(description = "设备类型")
    private String equipmentType;

    @Schema(description = "设备类型Id")
    private String equipmentTypeId;


    @Schema(description = "所在位置")
    private String equipmentLocation;

    @Schema(description = "出厂编码")
    private String factoryCode;

    @Schema(description = "父级设备id")
    private String parentEquipId;

    @Schema(description = "是否是物联设备")
    private Boolean iot;

    @Schema(description = "设备负责人")
    private String head;

    @Schema(description = "设备型号")
    private String eqmodelcode;

    @Schema(description = "设备所在位置名称")
    private String equipmentLocationName;



}
