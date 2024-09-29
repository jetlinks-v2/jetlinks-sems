package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.persistence.Column;
import java.math.BigDecimal;


@Data
public class EnergyReturnRes {

    /**
     * 上报时间
     */
    private Long gatherTime;
    /**
     * 用能
     */
    private BigDecimal difference;
    /**设备位置*/

    private String addr;

    /**区域id*/
    private String areaId;

    /**区域名称*/
    private String areaName;

    /**设备名称*/
    private String deviceName;

    /**
     * 设备id
     */
    private String deviceId;

    /**试验名称*/
    private String testName;


}
