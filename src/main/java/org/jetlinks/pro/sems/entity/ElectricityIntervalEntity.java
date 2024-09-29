package org.jetlinks.pro.sems.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.pro.sems.entity.res.CostconfigTimeRes;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.List;

@Table(name = "electricity_cost_config")
@Getter
@Setter
@Schema(description = "电生效区间配置")
@EnableEntityEvent //开启实体类crud事件
public class ElectricityIntervalEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column(length = 500)
    @Schema(description = "配置id")
    private String costConfigId;

    @Column(length = 500)
    @Schema(description = "电生效时间年开始")
    private Long yearStart;

    @Column(length = 500)
    @Schema(description = "电生效时间年结束")
    private Long yearEnd;

    @Column(length = 500)
    @Schema(description = "电生效时间月")
    private String month;



    @Column(length = 500)
    @Schema(description = "尖时时段")
    @JsonCodec
    //使用json字符串来存储map,因此数据库中使用LONGVARCHAR来存储
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    private List<CostconfigTimeRes> cuspPeriods;

    @Column(length = 500)
    @Schema(description = "峰时时段")
    @JsonCodec
    //使用json字符串来存储map,因此数据库中使用LONGVARCHAR来存储
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    private List<CostconfigTimeRes> peakPeriods;



    @Column(length = 500)
    @Schema(description = "平时时段")
    @JsonCodec
    //使用json字符串来存储map,因此数据库中使用LONGVARCHAR来存储
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    private List<CostconfigTimeRes> flatPeriods;


    @Column(length = 500)
    @Schema(description = "谷时时段")
    @JsonCodec
    //使用json字符串来存储map,因此数据库中使用LONGVARCHAR来存储
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    private List<CostconfigTimeRes> valleyPeriods;


    @Column(length = 10)
    @DefaultValue("1")
    @Schema(description = "状态 1：没有删除，2：已删除")
    private Long state;

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
