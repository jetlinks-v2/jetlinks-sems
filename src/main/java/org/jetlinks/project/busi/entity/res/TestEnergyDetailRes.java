package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.jetlinks.pro.io.excel.annotation.ExcelHeader;
import org.jetlinks.project.busi.entity.GasConsumeEntity;
import org.jetlinks.project.busi.entity.TestEnergyDetailEntity;

import javax.persistence.Column;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TestEnergyDetailRes {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "试验开始时间")
    private Long testStartTime;

    @Schema(description = "试验结束时间")
    private Long testEndTime;

    @Schema(description = "试验记录ID")
    private String testRecordId;

    @Schema(description = "水")
    private BigDecimal water;

    @Schema(description = "电")
    private BigDecimal electricity;

    @Schema(description = "气")
    private BigDecimal gas;

    @Column(precision = 8,scale = 2)
    @Schema(description = "水费用")
    private BigDecimal waterPrice;

    @Column(precision = 8,scale = 2)
    @Schema(description = "电费用")
    private BigDecimal electricityPrice;

    @Column(precision = 8,scale = 2)
    @Schema(description = "气费用")
    private BigDecimal gasPrice;

    List<TestEnergyDetailEntity> energyDetailList;

}
