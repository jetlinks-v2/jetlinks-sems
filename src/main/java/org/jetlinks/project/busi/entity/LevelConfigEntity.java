package org.jetlinks.project.busi.entity;

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
import org.jetlinks.project.busi.enums.EnergyQueryTypeEnum;
import org.jetlinks.project.busi.enums.EnergyType;

import javax.persistence.Column;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.sql.JDBCType;

/**
 * @author 13180
 * @see
 * @see
 * @see
 * @since 1.0
 */
@Table(name = "sems_energy_level_config")
@Getter
@Setter
@Schema(description = "能源等级配置表")
@EnableEntityEvent //开启实体类crud事件
public class LevelConfigEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column(length = 60,nullable = false)
    @EnumCodec(toMask = true)
    @ColumnType(javaType = Long.class,jdbcType = JDBCType.BIGINT)
    @Schema(description = "类型:1区域 2试验")
    private EnergyQueryTypeEnum type;

    @Column(length = 64)
    @Schema(description = "能耗等级",nullable = false)
    private String level;

    @Column(length = 64,nullable = false)
    @Schema(description = "能耗颜色")
    private String color;

    @Column(precision = 8,scale = 2,nullable = false)
    @Schema(description = "能耗范围起始值")
    private BigDecimal startNumber;

    @Column(precision = 8,scale = 2,nullable = false)
    @Schema(description = "能耗范围结束值")
    private BigDecimal endNumber;

    @Column(length = 60,nullable = false)
    @EnumCodec(toMask = true)
    @ColumnType(javaType = Long.class,jdbcType = JDBCType.BIGINT)
    @Schema(description = "能源类型")
    private EnergyType energyType;

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
