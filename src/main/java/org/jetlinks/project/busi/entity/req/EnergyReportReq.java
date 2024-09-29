package org.jetlinks.project.busi.entity.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.jetlinks.project.busi.enums.EnergyType;

import javax.persistence.Column;
import java.sql.JDBCType;

@Data
public class EnergyReportReq {

    @Schema(description = "能源类型")
    private EnergyType energyType;

    @Schema(description = "区域path")
    private String path;

    @Schema(description = "查询时间区间")
    private Long[] gatherTime;

    private Integer pageIndex;

    private Integer pageSize;


}
