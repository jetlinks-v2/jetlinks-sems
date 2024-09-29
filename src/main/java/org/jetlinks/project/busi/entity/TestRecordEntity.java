package org.jetlinks.project.busi.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.reactor.excel.CellDataType;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.pro.io.excel.annotation.ExcelHeader;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Table(name = "sems_test_record")
@Getter
@Setter
@Schema(description = "试验记录表")
@EnableEntityEvent //开启实体类crud事件
public class TestRecordEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column(length = 200,nullable = false)
    @Schema(description = "名称")
    @ExcelHeader
    @NotNull(message = "试验记录名称为必填字段！请输入准确的名称再导入！")
    @Length(max = 200,message = "试验记录名称超过200字符，请修改后重新导入")
    private String name;

    @Column(length = 64,nullable = false)
    @Schema(description = "试验配置ID")
    private String configId;

    @Column(length = 64)
    @Schema(description = "试验配置名称")
    @ExcelHeader
    @NotNull(message = "试验配置名称为必填字段！请输入准确的名称再导入！")
    @Length(max = 64,message = "试验配置名称超过64字符，请修改后重新导入")
    private String testName;

    @Column(length = 20)
    @Schema(description = "试验类型")//1-风阻 2-其他气动  3-风噪  4-其他声学
    @NotNull(message = "试验类型为必填字段！请输入准确的试验类型再导入！")
    @ExcelHeader
    private String testType;

    @Column
    @Schema(description = "试验开始时间")
    @ExcelHeader(dataType = CellDataType.DATE_TIME)
    @NotNull(message = "试验开始时间为必填字段！请输入准确的时间再导入！")
    private Long testStartTime;

    @Column
    @Schema(description = "试验结束时间")
    @ExcelHeader(dataType = CellDataType.DATE_TIME)
    @NotNull(message = "试验结束时间为必填字段！请输入准确的时间再导入")
    private Long testEndTime;

    @Column(length = 64)
    @Schema(description = "试验人员ID")
    private String testPeopleId;

    @Column(length = 200)
    @Schema(description = "试验人员名称")
    @ExcelHeader
    @Length(max = 200,message = "试验人员名称超过200字符，请修改后重新导入")
    private String tester;

    @Column(precision = 8,scale = 2)
    @Schema(description = "试验费用")
    @DefaultValue(value = "0")
    private BigDecimal testExpenses;

    @Column(length = 255)
    @Schema(description = "试验项目id")
    private String testProjId;

    @Column(length = 500)
    @Schema(description = "试验场所")
    private String testAreaList;

    @Column(length = 500)
    @Schema(description = "描述")
    @ExcelHeader
    @Length(max = 500,message = "描述超过500字符，请修改后重新导入")
    private String descr;

    @Column(length = 1)
    @DefaultValue("0")
    @Schema(description = "试验记录状态即条目状态 0未开始 1进行中 2结束")
    private String itemStatus;

    @Column(length = 255)
    @Schema(description = "项目下面对应的条目id")
    private String itemId;

    @Column(length = 1)
    @DefaultValue("0")
    @Schema(description = "状态")
    private String status;

    //试验预约数据有这两个状态，每次同步数据他预约的状态可能改变，如果对预测数据没影响可以不要
    @Column(length = 1)
    @Schema(description = "试验预约取消状态 0未取消1 已取消")
    private String cancelStatus;

    @Column(length = 1)
    @Schema(description = "试验类型 0 非预约试验 1 预约试验")
    private String recordType;

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
