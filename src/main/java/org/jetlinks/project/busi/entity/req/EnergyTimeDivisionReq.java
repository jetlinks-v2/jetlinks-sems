package org.jetlinks.project.busi.entity.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetlinks.project.busi.enums.EnergyQueryTypeEnum;
import org.jetlinks.project.busi.enums.EnergyType;

/**分时能耗请求参数
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.0
 */
@Data
public class EnergyTimeDivisionReq {
    /**能源查询类型*/
    @Schema(description = "能源查询类型", example = "area")
    EnergyQueryTypeEnum queryType;
    /**能耗排行查询名*/
    @Schema(description = "能耗排行查询名", example = "南岸区")
    private String queryName;
    /**能源类型：1水，2电，3气*/
    @Schema(description = "能源类型",example = "water")
    private EnergyType energyType;
    @Schema(description = "查询开始时间")
    private Long startDate;
    /**结束时间*/
    @Schema(description = "查询结束时间")
    private Long endDate;
    /**起始页*/
    @Schema(description = "起始页")
    private Integer pageIndex;
    /**结束时间*/
    @Schema(description = "页大小")
    private Integer pageSize;

}
