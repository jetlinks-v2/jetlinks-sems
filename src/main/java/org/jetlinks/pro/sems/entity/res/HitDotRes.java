package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class HitDotRes {

    @Schema(description = "井房/设备ID")
    private String houseDeviceId;

    @Schema(description = "名字")
    private String name;

    @Schema(description = "坐标")
    private String coordinate;

    @Schema(description = "楼层")
    private String floor;

    @Schema(description = "启用状态 1-井房，2-设备，3-电表,4-水表,5-气表,6-其他电表,7-其他水表,8-其他气表")
    private String type;

    @Schema(description = "告警编码")
    private String alarmCode;

    @Schema(description = "状态:0未处理，1已处理")
    private String alarmStatus;
}
