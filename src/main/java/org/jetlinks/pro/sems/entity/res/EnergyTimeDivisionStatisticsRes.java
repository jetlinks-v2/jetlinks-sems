package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**分时能耗统计
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.0
 */
@Data
public class EnergyTimeDivisionStatisticsRes {
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
    @Schema(description = "水/气：用量")
    private BigDecimal number;
}
