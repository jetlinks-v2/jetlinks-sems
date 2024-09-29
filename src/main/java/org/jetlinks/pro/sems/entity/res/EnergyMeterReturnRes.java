package org.jetlinks.pro.sems.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.reactor.excel.CellDataType;
import org.jetlinks.pro.io.excel.annotation.ExcelHeader;
import org.jetlinks.pro.sems.enums.EnergyType;

import javax.persistence.Column;
import java.math.BigDecimal;


@Data
public class EnergyMeterReturnRes  {

    @Schema(description = "表计名称")
    @ExcelHeader
    private String deviceName;

    @Schema(description = "表计编号")
    @ExcelHeader
    private String deviceId;

    @Column
    //枚举
    @EnumCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "能源类型")
    @ExcelHeader
    private EnergyType energyType;

    @Schema(description = "上次抄表时间")
    @ExcelHeader(dataType = CellDataType.DATE_TIME)
    private Long lastMeterTime;


    @Schema(description = "上次抄表数")
    @ExcelHeader
    private BigDecimal lastMeterNum;


    @Schema(description = "本次抄表时间")
    @ExcelHeader(dataType = CellDataType.DATE_TIME)
    private Long thisMeterTime;


    @Schema(description = "本次抄表数")
    @ExcelHeader
    private BigDecimal thisMeterNum;



    @Schema(description = "用量")
    @ExcelHeader
    private BigDecimal difference;




}
