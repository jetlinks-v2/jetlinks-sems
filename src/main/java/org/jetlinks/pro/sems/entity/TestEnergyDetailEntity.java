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

@Table(name = "sems_test_energy_detail")
@Getter
@Setter
@Schema(description = "试验能耗详情表")
@EnableEntityEvent //开启实体类crud事件
public class TestEnergyDetailEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column(length = 64,nullable = false)
    @Schema(description = "试验记录ID")
    private String testRecordId;

    @Column(length = 64,nullable = false)
    @Schema(description = "设备ID")
    private String deviceId;

    @Column(length = 200,nullable = false)
    @Schema(description = "设备名称")
    private String deviceName;

    @Column(length = 1,nullable = false)
    @Schema(description = "是否共用设备 0-否，1-是")
    @DefaultValue("0")
    private String shareDevice;

    @Column(precision = 8,scale = 2)
    @Schema(description = "水")
    @DefaultValue(value = "0")
    private BigDecimal water;

    @Column(precision = 8,scale = 2)
    @Schema(description = "水费用")
    @DefaultValue(value = "0")
    private BigDecimal waterPrice;

    @Column(precision = 8,scale = 2)
    @Schema(description = "电")
    @DefaultValue(value = "0")
    private BigDecimal electricity;

    @Column(precision = 8,scale = 2)
    @Schema(description = "电费用")
    @DefaultValue(value = "0")
    private BigDecimal electricityPrice;

    @Column(precision = 8,scale = 2)
    @Schema(description = "气")
    @DefaultValue(value = "0")
    private BigDecimal gas;

    @Column(precision = 8,scale = 2)
    @Schema(description = "气费用")
    @DefaultValue(value = "0")
    private BigDecimal gasPrice;

    @Column(length = 500)
    @DefaultValue("0")
    @Schema(description = "描述")
    private String descr;

    @Column(length = 1)
    @DefaultValue("0")
    @Schema(description = "状态")
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
