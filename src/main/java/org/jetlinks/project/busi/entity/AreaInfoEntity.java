package org.jetlinks.project.busi.entity;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.pro.io.excel.annotation.ExcelHeader;
import org.springframework.context.annotation.Description;

import javax.persistence.Column;
import javax.persistence.Table;
import java.util.List;

@Table(name = "area_info")
@Getter
@Setter
@Schema(description = "区域信息表")
@EnableEntityEvent //开启实体类crud事件
public class AreaInfoEntity extends GenericTreeSortSupportEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column(length = 64,nullable = false)
    @Schema(description = "区域名称")
    @ExcelHeader
    @NotBlank(message = "该条记录的区域名称为必填字段！请输入准确的名称再导入！")
    @Length(max = 64,message = "区域名称超过64位，请修改后重新导入")
    private String areaName;


    @Column(length = 64,nullable = false)
    @Schema(description = "区域地址")
    @ExcelHeader(value = "地址信息")
    @Length(max = 64,message = "区域地址超过64位，请修改后重新导入")
    @NotBlank(message = "该条记录的区域地址为必填字段！请输入准确的名称再导入！")
    private String addr;

    @Column(length = 200)
    @Schema(description = "区域描述")
    @Length(max = 200,message = "描述字段超过200位，请修改后重新导入")
    @ExcelHeader(value = "描述")
    private String descr;


    @Column(length = 1,nullable = false)
    @DefaultValue(value = "0")
    @Schema(description = "记录状态")
    private String state;

    @Column(length = 500)
    @Schema(description = "备注")
    private String remark;

    @Column(length = 64)
    @Schema(description = "父id")
    @DefaultValue("0")
    private String parentId;

    @Column(length = 1)
    @Schema(description = "绑定状态")
    private String bindState;


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

    @Schema(description = "子集")
    private List<AreaInfoEntity> children;

    private List<DeviceInfoEntity> deviceChildren;

    @Schema(description = "变更表日志记录")
    private List<ChangeRecordEntity> changeRecordEntity;


}
