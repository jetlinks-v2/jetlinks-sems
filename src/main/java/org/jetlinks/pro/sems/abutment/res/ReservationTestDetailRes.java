package org.jetlinks.pro.sems.abutment.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ReservationTestDetailRes {

    @Schema(description = "取消状态 0未取消 1已取消")
    private String cancelStatus;

    @Schema(description = "预约试验结束时间（到分钟如 2023-01-01 12:21)")
    private String experimentEndTime;

    @Schema(description = "预约试验开始时间（到分钟如\n" +
        "2023-01-01 12:21）")
    private String experimentStartTime;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "预约所选条目集合")
    private List<ItemDateRes> itemList;
}
