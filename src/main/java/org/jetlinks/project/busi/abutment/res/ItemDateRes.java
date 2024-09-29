package org.jetlinks.project.busi.abutment.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetlinks.project.busi.entity.TestAreaEntity;

import java.util.List;


@Data
public class ItemDateRes {

    @Schema(description = "条目对应场地")
    private List<Integer> areaIdList;

    @Schema(description = "条目对应设备")
    private List<Integer> deviceIdList;

    @Schema(description = "条目结束时间（到分钟如2023-" +
        "01-01 12:21）")
    private String experimentEndTime;

    @Schema(description = " 条目开始时间（到分钟 如 2023-" +
        "01-01 12:21）")
    private String experimentStartTime;

    @Schema(description = "项目对应条目id")
    private String id;

    @Schema(description = "条目表主键id")
    private String experimentItemId;

    @Schema(description = "条目状态 0未开始 1进行中 2结束")
    private String itemStatus;

    @Schema(description = "条目对应人员")
    private String peopleIdList;

    @Schema(description = "试验人员")
    private String peopleName;

    @Schema(description = "场所列表")
    private List<TestAreaEntity> testAreaEntityList;

    @Schema(description = "设备列表")
    private List<ExperimentDeviceRes> deviceResList;

    @Schema(description = "条目名称")
    private String itemName;

}
