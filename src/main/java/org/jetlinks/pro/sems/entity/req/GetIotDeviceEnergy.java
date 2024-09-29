package org.jetlinks.pro.sems.entity.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class GetIotDeviceEnergy {

    @Schema(description = "同步水气的时候区分是水还是气，0：水，1：气")
    private String tag;

    @Schema(description = "设备id，同步某一个设备的时候传，批量同步不需要传")
    private String deviceId;

    @Schema(description = "0 全部表，1设备表 2：总表，批量同步的时候用")
    private String type;

    @Schema(description = "属性，同步水和气的时候要用")
    private String property;

    @Schema(description = "开始时间")
    private  Long startTime;

    @Schema(description = "结束时间")
    private Long endTime;

}
