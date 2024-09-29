package org.jetlinks.project.busi.entity;




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

import java.util.List;



@Table(name = "cost_config")
@Getter
@Setter
@Schema(description = "费用配置表")
@EnableEntityEvent //开启实体类crud事件
public class CostConfigEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column(nullable = false)
    @Schema(description = "费用配置名称列表")
    private String costConfigNameId;



    @Column(nullable = false,length = 1)
    @Schema(description = "费用类型，1：水，2：电，3：气")
    private String energyType;

    @Column(precision = 5,scale = 2)
    @Schema(description = "价格")
    private Float unitPrice;


    @Column(length = 500)
    @Schema(description = "生效时间区间开始时间")
    private Long effectiveTimeIntervalStartDate;

    @Column(length = 500)
    @Schema(description = "生效时间区间结束时间")
    private Long effectiveTimeIntervalEndDate;

    @Column(length = 500)
    @Schema(description = "基准电价实行峰谷浮动")
    private String referenceElectricityPriceFloat;

    @Column(length = 500)
    @Schema(description = "基准电价")
    private String referencePrice;

    @Column(length = 500)
    @Schema(description = "基准电价上浮")
    private String referencePriceFloat;

    @Column(length = 500)
    @Schema(description = "其他月份上浮")
    private String otherMonthFloat;

    @Column(length = 500)
    @Schema(description = "高峰电价在基准电价的上浮")
    private String peakOnReferenceFloat;

    @Column(length = 500)
    @Schema(description = "低谷时段在基准电价的上浮")
    private String lowOnReferenceFloat;

    @Column(length = 500)
    @Schema(description = "电生效区间id")
    private String electricityConfigId;

    @Column(length = 10)
    @DefaultValue("1")
    @Schema(description = "状态 1：没有删除，2：已删除")
    private Long state;


    @Schema(description = "费用配置名称")
    private String costName;

    @Schema(description = "费用配置名称描述")
    private String costRemark;

    private List<ElectricityIntervalEntity> electricityIntervalEntities;

    private List<CostConfigEntity> waterOrGasList;





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
