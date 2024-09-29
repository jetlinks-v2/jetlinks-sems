package org.jetlinks.pro.sems.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.pro.io.excel.annotation.ExcelHeader;
import org.jetlinks.pro.sems.entity.res.EnergyRatioRes;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.sql.JDBCType;
import java.util.List;

@Table(name = "sems_energy_ratio")
@Data
@Schema(description = "能耗占比配置表")
@EnableEntityEvent //开启实体类crud事件
public class EnergyRatioEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column(length = 64,nullable = false)
    @Schema(description = "试验条目ID")
    private String configId;

    @Column(length = 64)
    @Schema(description = "试验条目名称")
    @ExcelHeader
    @NotNull(message = "试验条目名称为必填字段！请输入准确的名称再导入！")
    @Length(max = 64,message = "试验条目名称超过64字符，请修改后重新导入")
    private String testConfigName;

    @Column(length = 1)
    @DefaultValue("0")
    @Schema(description = "启用状态 0-关，1-开")
    private String configStatus;

    @Column(length = 500)
    @Schema(description = "空调占比")
    @JsonCodec
    //使用json字符串来存储map,因此数据库中使用LONGVARCHAR来存储
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    private List<EnergyRatioRes> ratioAirCondition;

    @Column(length = 500)
    @Schema(description = "普冷占比")
    @JsonCodec
    //使用json字符串来存储map,因此数据库中使用LONGVARCHAR来存储
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    private List<EnergyRatioRes> ordinaryFreezing;

    @Column(length = 500)
    @Schema(description = "组合开始冷却塔占比-自动")
    @JsonCodec
    //使用json字符串来存储map,因此数据库中使用LONGVARCHAR来存储
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    private List<EnergyRatioRes> combinedCoolingTowerAuto;

    @Column(length = 500)
    @Schema(description = "组合开始冷却塔占比-手动")
    @JsonCodec
    //使用json字符串来存储map,因此数据库中使用LONGVARCHAR来存储
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    private List<EnergyRatioRes> combinedCoolingTower;

    @Column(length = 500)
    @Schema(description = "锅炉占比")
    @JsonCodec
    //使用json字符串来存储map,因此数据库中使用LONGVARCHAR来存储
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    private List<EnergyRatioRes> boiler;

    @Column(length = 500)
    @Schema(description = "空压机占比-气动")
    @JsonCodec
    //使用json字符串来存储map,因此数据库中使用LONGVARCHAR来存储
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    private List<EnergyRatioRes> airCompressorPneumatic;

    @Column(length = 500)
    @Schema(description = "空压机占比-声学")
    @JsonCodec
    //使用json字符串来存储map,因此数据库中使用LONGVARCHAR来存储
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    private List<EnergyRatioRes> airCompressor;

    @Column(length = 1)
    @DefaultValue("0")
    @Schema(description = "是否删除 0-否，1-是")
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
}
