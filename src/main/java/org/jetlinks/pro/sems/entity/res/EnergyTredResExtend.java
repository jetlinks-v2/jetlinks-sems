package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EnergyTredResExtend {
    /*时间*/
    @Schema(description = "时间")
    private String time;

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

    /*水/气用量*/
    @Schema(description = "水/气：峰值")
    private BigDecimal peakNum;

    /*水/气用量*/
    @Schema(description = "水/气：波谷")
    private BigDecimal lowNum;

    private String type;

    private BigDecimal num;
}
