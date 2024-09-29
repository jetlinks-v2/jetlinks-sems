package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetlinks.project.busi.entity.AreaInfoEntity;

import java.math.BigDecimal;
import java.util.List;

/**分时能耗清单返回参数
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.0
 */
@Data
public class EnergyTimeDivisionAccountRes {
    /*区域id*/
    @Schema(description = "区域id")
    private String id;
    /*区域名称*/
    @Schema(description = "区域名称")
    private String areaName;
    /*试验名称*/
    @Schema(description = "区域名称")
    private String testName;
    /*电能耗*/
    @Schema(description = "电能耗")
    private BigDecimal totalElectricityConsumption;
    /*水能耗*/
    @Schema(description = "水能耗")
    private BigDecimal totalWaterConsumption;
    /*气能耗*/
    @Schema(description = "气能耗")
    private BigDecimal totalGasConsumption;
    /*日期*/
    @Schema(description = "日期")
    private String date;
    /*子集*/
    @Schema(description = "子集")
    private List<AreaInfoEntity> children;
}
