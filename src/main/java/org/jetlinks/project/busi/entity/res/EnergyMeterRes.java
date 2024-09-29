package org.jetlinks.project.busi.entity.res;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EnergyMeterRes {

    /**设备名称*/
    private String deviceId;

    /**设备名称*/
    private String deviceName;

    /**区域id*/
    private String areaId;

    /**区域名称*/
    private String areaName;

    /**上次抄表时间*/
    private Long lastMeterTime;

    /**上次抄表数值*/
    private BigDecimal lastMeterNum;

    /**本次抄表时间*/
    private Long thisMeterTime;

    /**本次抄表数值*/
    private BigDecimal thisMeterNum;

    /**用量*/
    private BigDecimal difference;
}
