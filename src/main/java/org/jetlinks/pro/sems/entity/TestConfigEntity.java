package org.jetlinks.pro.sems.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.pro.io.excel.annotation.ExcelHeader;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Table(name = "sems_test_config")
@Getter
@Setter
@Schema(description = "试验配置表")
@EnableEntityEvent //开启实体类crud事件
public class TestConfigEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column(length = 64,nullable = false)
    @Schema(description = "试验名称")
    @ExcelHeader
    @NotNull(message = "试验名称为必填字段！请输入准确的名称再导入！")
    @Length(max = 64,message = "试验名称超过64字符，请修改后重新导入")
    private String testName;

    @Column(length = 500)
    @Schema(description = "描述")
    @ExcelHeader
    @Length(max = 500,message = "描述信息超过500字符，请修改后重新导入")
    private String descr;

    @Column(length = 1,nullable = false)
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
