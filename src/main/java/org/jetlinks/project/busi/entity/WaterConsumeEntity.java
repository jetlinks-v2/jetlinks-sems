package org.jetlinks.project.busi.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.generator.Generators;

import javax.persistence.Column;
import javax.persistence.Table;
import java.math.BigDecimal;

@Table(name = "sems_water_consume")
@Getter
@Setter
@Schema(description = "用水信息表")
public class WaterConsumeEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

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

    @Column(length = 1,nullable = false)
    @DefaultValue("0")
    @Schema(description = "记录状态")
    private String status;


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
}
