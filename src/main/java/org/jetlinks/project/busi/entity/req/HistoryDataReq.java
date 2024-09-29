package org.jetlinks.project.busi.entity.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetlinks.project.busi.enums.EnergyType;

/**历史数据查询参数
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.0
 */
@Data
public class HistoryDataReq {
    @Schema(description = "能源类型")
    private EnergyType energyType;
    /**设备名称*/
    @Schema(description = "设备名称")
    private String deviceName;
    /**开始时间*/
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
