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
import java.math.BigDecimal;
import java.sql.JDBCType;
import java.util.List;

@Table(name = "sems_device_info")
@Getter
@Setter
@Schema(description = "设备信息表")
@EnableEntityEvent //开启实体类crud事件
public class DeviceInfoEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column(length = 64,nullable = false)
    @Schema(description = "设备ID")
    private String deviceId;

    @Column(length = 64,nullable = false)
    @DefaultValue(value = "0")
    @Schema(description = "父设备ID")
    private String parentId;

    @Column(length = 200,nullable = false)
    @Schema(description = "设备名称")
    private String deviceName;

    @Column(length = 200)
    @Schema(description = "设备型号")
    private String deviceModel;

    @Column(length = 200)
    @Schema(description = "设备型号id")
    private String deviceModelId;

    @Column(length = 60,nullable = false)
    @EnumCodec(toMask = true)
    @ColumnType(javaType = Long.class,jdbcType = JDBCType.BIGINT)
    @Schema(description = "能源类型")
    private EnergyType[] energyType;

    @Column(length = 64)
    @Schema(description = "所属区域")
    private String areaId;

    @Column(length = 64)
    @Schema(description = "场所id")
    private String placeId;

    @Column(length = 100)
    @Schema(description = "出厂编码")
    private String factoryNumber;

    @Column(length = 100)
    @Schema(description = "设备负责人")
    private String duty;

    @Column(length = 1)
    @DefaultValue(value = "0")
    @Schema(description = "重点设备 0-否，1-是")
    private String importance;

    @Column(length = 1,nullable = false)
    @DefaultValue("0")
    @Schema(description = "运行状态 0-离线，1-在线")
    private String deviceStatus;

    @Column(length = 1)
    @DefaultValue("4")
    @Schema(description = "设备状态 0-未知状态 1-开机 2-关机 3-运行 4-停止 5-报警 6-故障")
    private String computeStatus;

    @Column(precision = 20,scale = 2)
    @Schema(description = "初始表数")
    private BigDecimal meterNumber;

    @Column
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    @Schema(description = "图片地址")
    private String filePath;

    @Column(length = 1,nullable = false)
    @DefaultValue("0")
    @Schema(description = "记录状态 是否删除 0-否，1-是，2-更换")
    private String status;

    @Column(length = 1,nullable = false)
    @DefaultValue("0")
    @Schema(description = "父id绑定的是表还是区域，0是表，1是区域")
    private String parentFlag;

    @Column(length = 1)
    @DefaultValue("0")
    @Schema(description = "设备类型:0-设备，1-表计设备")
    private String deviceType;

    @Column(length = 1)
    @Schema(description = "表类型:0-总表，1-子表")
    private String meterType;

    @Column(length = 1,nullable = false)
    @Schema(description = "是否共用设备 0-否，1-是")
    @DefaultValue("0")
    private String shareDevice;

    @Column(length = 1)
    @Schema(description = "订阅状态 0-否，1-是")
    @DefaultValue("0")
    private String subscribeStatus;

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

    @Column(length = 200)
    @Schema(description = "旧设备id，多个用,隔开")
    private String oldDeviceIds;

    private List<DeviceInfoEntity> child;

    @Schema(description = "变更表日志记录")
    private List<ChangeRecordEntity> changeRecordEntity;

    @Schema(description = "组合设备和用能设备组合需要返回组合设备的名称")
    private String areaName;
}
