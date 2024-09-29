package org.jetlinks.pro.sems.entity;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.pro.sems.enums.EnergyType;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.List;

@Table(name = "sems_combination_unit")
@Getter
@Setter
@Schema(description = "组合设备信息表")
@EnableEntityEvent //开启实体类crud事件
public class CombinationUnitEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column(length = 200,nullable = false)
    @Schema(description = "组合设备名称")
    private String unitName;


    @Column(length = 60,nullable = false)
    @EnumCodec(toMask = true)
    @ColumnType(javaType = Long.class,jdbcType = JDBCType.BIGINT)
    @Schema(description = "能源类型")
    private EnergyType[] energyType;

    @Column(length = 64)
    @Schema(description = "所属区域")
    private String areaId;

    private String areaName;

    @DefaultValue("0")
    @Column(length = 1)
    @Schema(description = "运行状态 0-离线，1-在线")
    private String deviceStatus;

    @Column(length = 1,nullable = false)
    @DefaultValue("0")
    @Schema(description = "记录状态")
    private String status;

    @Schema(description = "组合设备名称集")
    private List<String> deviceNames;


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
}
