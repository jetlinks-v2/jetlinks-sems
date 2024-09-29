package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.0
 */
@Data
public class HistoryDataInfo{
    /*设备名称*/
    @Schema(description = "设备名称")
    private String deviceName;
    /*统计时间*/
    @Schema(description = "统计时间")
    private String  statisticsDate;
    /*区域名*/
    @Schema(description = "区域名")
    private String  areaName;


    /*尖用量*/
    @Schema(description = "电：尖用量")
    private BigDecimal cuspNumber;



    /*峰用量*/
    @Schema(description = "电：峰用量")
    private BigDecimal peakNumber;



    /*平用量*/
    @Schema(description = "电：平用量")
    private BigDecimal flatNumber;



    /*谷用量*/
    @Schema(description = "电：谷用量")
    private BigDecimal valleyNumber;


    /*开始表数*/
    @Schema(description = "水/气：开始表数")
    private BigDecimal startNumber;
    /*结束表数*/
    @Schema(description = "水/气：结束表数")
    private BigDecimal endNumber;
    /*用量*/
    @Schema(description = "水/气：用量")
    private BigDecimal totalNumber;

}
