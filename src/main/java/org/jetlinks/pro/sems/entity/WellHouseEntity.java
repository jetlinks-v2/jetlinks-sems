package org.jetlinks.pro.sems.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

@Table(name = "sems_well_house")
@Data
@Schema(description = "井房表")
@EnableEntityEvent //开启实体类crud事件
public class WellHouseEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column(length = 200,nullable = false)
    @Schema(description = "名称")
    @NotBlank(message = "名称不能为空")
    private String name;

    @Column(length = 200)
    @Schema(description = "位置")
    private String position;

    @Column(length = 200)
    @Schema(description = "能源类型")
    private String energyType;

    @Column(length = 1)
    @DefaultValue("0")
    @Schema(description = "启用状态 0-未用，1-在用")
    private String enableStatus;

    @Column(length = 500)
    @Schema(description = "描述")
    private String descr;

    @Column(length = 1)
    @DefaultValue("0")
    @Schema(description = "是否删除 0-否，1-是")
    private String status;

    @Column(length = 500)
    @Schema(description = "备注")
    private String remark;

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
}
