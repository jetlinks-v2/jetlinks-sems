package org.jetlinks.project.busi.entity.res;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TestRecordEnergyRes {

    @Schema(description = "试验记录id")
    private String recordId;

    @Schema(description = "试验条目id")
    private String configId;

    @Schema(description = "条目名称")
    private String configName;

    @Schema(description = "电能耗")
    private BigDecimal electricityEnergy;

    @Schema(description = "电能耗费用")
    private BigDecimal electricityCost;

    @Schema(description = "水能耗")
    private BigDecimal waterEnergy;

    @Schema(description = "水能耗费用")
    private BigDecimal waterCost;

    @Schema(description = "气能耗")
    private BigDecimal gasEnergy;

    @Schema(description = "气能耗费用")
    private BigDecimal gasCost;

}
