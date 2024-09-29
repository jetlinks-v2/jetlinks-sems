package org.jetlinks.project.busi.entity.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hswebframework.reactor.excel.CellDataType;
import org.jetlinks.pro.io.excel.annotation.ExcelHeader;

@Data
public class AlarmRecordsExcelRes {

    @Schema(description = "告警编码")
    @ExcelHeader
    private String alarmCode;

    @Schema(description = "告警规则名称")
    @ExcelHeader
    private String ruleName;

    @Schema(description = "告警维度 0-设备 1-试验 2-场所")
    @ExcelHeader(value = "告警维度" )
    private String alarmType;

    @Schema(description = "告警内容")
    @ExcelHeader
    private String alarmContent;

    @Schema(description = "告警时间")
    @ExcelHeader(dataType = CellDataType.DATE_TIME,format = "YYYY-mm-dd HH:mm:ss")
    private Long alarmTime;

    @Schema(description = "处理结果")
    @ExcelHeader
    private String disposeResult;

    @Schema(description = "处理人")
    @ExcelHeader
    private String disposePerson;

    @Schema(description = "处理时间")
    @ExcelHeader(dataType = CellDataType.DATE_TIME,format = "YYYY-mm-dd HH:mm:ss")
    private Long disposeTime;

}
