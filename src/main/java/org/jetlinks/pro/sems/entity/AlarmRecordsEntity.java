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
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Table(name = "sems_alarm_records")
@Getter
@Setter
@Schema(description = "告警记录表")
@EnableEntityEvent //开启实体类crud事件
public class AlarmRecordsEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column(length = 16,nullable = false)
    @Schema(description = "告警编码")
    private String alarmCode;

    @Column(length = 64,nullable = false)
    @Schema(description = "规则ID")
    private String ruleId;

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

    @Column(precision = 10,scale = 2)
    @Schema(description = "阈值")
    private BigDecimal threshold;

    @Column(precision = 8,scale = 2,nullable = false)
    @Schema(description = "当前值")
    private BigDecimal currentValue;

    @Column(nullable = false)
    @Schema(description = "告警时间")
    private Long alarmTime;

    @Column(length = 500)
    @Schema(description = "告警内容")
    private String alarmContent;

    @Column(length = 500)
    @Schema(description = "处理结果")
    @NotBlank(message = "处理结果不能为空")
    private String disposeResult;

    @Column(length = 100)
    @Schema(description = "处理人")
    private String disposePerson;

    @Column
    @Schema(description = "处理时间")
    private Long disposeTime;

    @Column(length = 500)
    @Schema(description = "描述")
    private String descr;

    @Column(length = 1,nullable = false)
    @DefaultValue("0")
    @Schema(description = "状态:0未处理，1已处理")
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
