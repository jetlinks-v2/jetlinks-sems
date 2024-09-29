package org.jetlinks.pro.sems.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;

import javax.persistence.Column;
import javax.persistence.Table;
import java.math.BigDecimal;

@Table(name = "sems_electricity_consume")
@Getter
@Setter
@Schema(description = "用电信息表")
@EnableEntityEvent //开启实体类crud事件
public class ElectricityConsumeEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column(length = 64)
    @Schema(description = "上报记录ID")
    private String gatherId;

    @Column(length = 64,nullable = false)
    @Schema(description = "设备ID")
    private String deviceId;

    @Column(length = 64,nullable = false)
    @Schema(description = "上报设备ID,表id")
    private String reportDeviceId;

    @Column(nullable = false)
    @Schema(description = "上报时间")
    private Long gatherTime;

    @Column(length = 64)
    @Schema(description = "所属区域")
    private String areaId;

    @Column(precision = 8,scale = 2)
    @Schema(description = "表数插值(本次-上次)")
    private BigDecimal difference;

    @Column(precision = 20,scale = 2)
    @Schema(description = "本次表数")
    private BigDecimal number;

    @Column(precision = 10,scale = 2)
    @Schema(description = "频率")
    private BigDecimal frequency;

    @Column(precision = 10,scale = 2)
    @Schema(description = "总功率因数")
    private BigDecimal powerFactorTotal;

    @Column(precision = 10,scale = 2)
    @Schema(description = "三相电压不平衡度")
    private BigDecimal threeVoltage;

    @Column(precision = 10,scale = 2)
    @Schema(description = "三相电流不平衡度")
    private BigDecimal threePhase;

    @Column(precision = 10,scale = 2)
    @Schema(description = "A相电流")
    private BigDecimal phaseIA;

    @Column(precision = 10,scale = 2)
    @Schema(description = "B相电流")
    private BigDecimal phaseIB;

    @Column(precision = 10,scale = 2)
    @Schema(description = "C相电流")
    private BigDecimal phaseIC;

    @Column(precision = 10,scale = 2)
    @Schema(description = "A相电压")
    private BigDecimal phaseUA;

    @Column(precision = 10,scale = 2)
    @Schema(description = "B相电压")
    private BigDecimal phaseUB;

    @Column(precision = 10,scale = 2)
    @Schema(description = "C相电压")
    private BigDecimal phaseUC;

    @Column(precision = 10,scale = 2)
    @Schema(description = "A相有功功率")
    private BigDecimal activePA;

    @Column(precision = 10,scale = 2)
    @Schema(description = "B相有功功率")
    private BigDecimal activePB;

    @Column(precision = 10,scale = 2)
    @Schema(description = "C相有功功率")
    private BigDecimal activePC;

    @Column(precision = 10,scale = 2)
    @Schema(description = "总有功功率")
    private BigDecimal power;

    @Column(precision = 10,scale = 2)
    @Schema(description = "A相无功功率")
    private BigDecimal reactivePA;

    @Column(precision = 10,scale = 2)
    @Schema(description = "B相无功功率")
    private BigDecimal reactivePB;

    @Column(precision = 10,scale = 2)
    @Schema(description = "C相无功功率")
    private BigDecimal reactivePC;

    @Column(precision = 10,scale = 2)
    @Schema(description = "总无功功率")
    private BigDecimal reactivePTotal;

    @Column(length = 1,nullable = false)
    @DefaultValue("0")
    @Schema(description = "记录状态")
    private String status;

    @Column(length = 3)
    @Schema(description = "设备运行状态  1-组合开始冷却塔手动 2-组合开始冷却塔自动")
    private String deviceRunStatus;

    @Column(updatable = false)
    @Schema(
        description = "创建者ID(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorId;

    @Column(updatable = false)
    @Schema(
        description = "创建者名称(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorName;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(
        description = "创建时间(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long createTime;

    @Column(length = 64)
    @Schema(
        description = "修改人ID"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String modifierId;

    @Column(length = 64)
    @Schema(
        description = "修改人名称"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String modifierName;

    @Column
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(
        description = "修改时间"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long modifyTime;

    @Column(length = 500)
    @Schema(description = "备注")
    private String remark;


    @Column(length = 64)
    @Schema(description = "费用配置表ID")
    private String costId;

    @Column(precision = 8,scale = 4)
    @Schema(description = "对应单价")
    private BigDecimal unitPrice;


    @Column(length = 6)
    @Schema(description = "尖峰平谷标识")
    private Integer periodsType;

}

