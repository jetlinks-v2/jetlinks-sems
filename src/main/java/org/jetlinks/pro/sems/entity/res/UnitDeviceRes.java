package org.jetlinks.pro.sems.entity.res;

import lombok.Data;

@Data
public class UnitDeviceRes {


    private String deviceStatus;

    private String areaNames;

    private String unitId;

    private String deviceId;

    private String deviceName;

    private Double difference;

    private Double total;

    private Long gatherTime;

    private String count;
}
