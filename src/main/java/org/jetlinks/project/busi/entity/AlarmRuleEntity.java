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
import org.jetlinks.project.busi.enums.EnergyType;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Table(name = "sems_alarm_rule")
@Getter
@Setter
@Schema(description = "告警规则表")
@EnableEntityEvent //开启实体类crud事件
public class AlarmRuleEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column(length = 200)
    @Schema(description = "规则名称")
    private String ruleName;

    @Column(length = 20,nullable = false)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "能耗类型")
    @NotBlank(message = "能耗类型不能为空")
    private EnergyType energyType;

    @Column(length = 1)
    @DefaultValue("0")
    @Schema(description = "告警维度 0-设备 1-试验 2-场所")
    private String alarmType;

    @Column(length = 1)
    @Schema(description = "规则类型 0-能耗，1-功率(仅限 电)")
    private String ruleType;

    @Column(length = 64,nullable = false)
    @Schema(description = "告警维度ID 设备ID/试验ID/场所ID")
    private String alarmTypeId;

    @Column(length = 200)
    @Schema(description = "名称 设备名称/条目名称/场所名称")
    private String name;

    @Column(length = 1)
    @DefaultValue("0")
    @Schema(description = "超值设定 0-历史能耗最高值上浮，1-自定义")
    private String settingStatus;

    @Column(precision = 4,scale = 2)
    @Schema(description = "上浮百分比")
    private BigDecimal percentage;

    @Column(precision = 10,scale = 2)
    @Schema(description = "阈值")
    private BigDecimal threshold;

    @Column(precision = 1)
    @DefaultValue("0")
    @Schema(description = "防抖状态 0-否，1-是")
    private String antiShakeStatus;

    @Column(precision = 8)
    @Schema(description = "防抖时间(多少时间内)")
    private Long antiShakeTime;

    @Column(length = 500)
    @Schema(description = "描述")
    @NotBlank(message = "预警说明不能为空")
    private String descr;

    @Column(length = 1)
    @DefaultValue("0")
    @Schema(description = "规则状态 0-关，1-开")
    private String ruleStatus;

    @Column(length = 20)
    @DefaultValue("0")
    @Schema(description = "推送方式 0-短信 1-企业微信 2-邮件")
    private String pushType;

    @Column
    @Schema(description = "推送人")
    private String pushUserId;

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
