package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetlinks.pro.sems.entity.ElectricityConsumeEntity;
import org.jetlinks.pro.sems.entity.GasConsumeEntity;
import org.jetlinks.pro.sems.entity.WaterConsumeEntity;
import org.jetlinks.pro.sems.enums.PeriodsEnum;

import java.math.BigDecimal;

/**
 * @ClassName PeakUniversalData
 * @Author hky
 * @Time 2023/7/17 15:10
 * @Description
 **/
@Data
public class PeakUniversalData {

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "上报时间")
    private Long  gatherTime;

    @Schema(description = "水电气用量差值")
    private BigDecimal totalValue;

    @Schema(description = "配置表ID")
    private String costId;

    @Schema(description = "对应单价")
    private BigDecimal unitPrice;


    @Schema(description = "尖峰平谷标识(为电时才会有该属性)")
    private PeriodsEnum periodsEnum;

    @Schema(description = "成本")
    private BigDecimal usePrice;





    public PeakUniversalData(GasConsumeEntity entity) {
        this.deviceId = entity.getDeviceId();
        this.gatherTime = entity.getGatherTime();
        this.totalValue = entity.getDifference();
        this.costId = entity.getCostId();
        this.unitPrice = entity.getUnitPrice();
    }

    public PeakUniversalData(ElectricityConsumeEntity entity) {
        this.deviceId = entity.getDeviceId();
        this.gatherTime = entity.getGatherTime();
        this.totalValue = entity.getDifference();
        this.costId = entity.getCostId();
        this.unitPrice = entity.getUnitPrice();
        this.periodsEnum = PeriodsEnum.of(entity.getPeriodsType());
    }

    public PeakUniversalData(WaterConsumeEntity entity) {
        this.deviceId = entity.getDeviceId();
        this.gatherTime = entity.getGatherTime();
        this.totalValue = entity.getDifference();
        this.costId = entity.getCostId();
        this.unitPrice = entity.getUnitPrice();
    }
}
