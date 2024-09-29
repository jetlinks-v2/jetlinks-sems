package org.jetlinks.project.busi.abutment.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;

import java.util.List;

@Data
public class LocationRes {

    @Schema(description = "设备位置id")
    @DefaultValue("0")
    private String locationId;

    @Schema(description = "设备位置父id")
    private String locationPid;

    @Schema(description = " 设备位置名称")
    private String locationName;

    @Schema(description = "设备位置类型")
    private String locationType;

    @Schema(description = "详细地址")
    private String detailedAddress;

    @Schema(description = "备注")
    private String descr;

    @Schema(description = "1:未删，0删除")
    private Integer enable;

    private List<LocationRes> children;

}
