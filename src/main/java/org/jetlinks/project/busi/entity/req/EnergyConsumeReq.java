package org.jetlinks.project.busi.entity.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;

@Data
public class EnergyConsumeReq {

    QueryParamEntity query;
    @Schema(description = "统计维度:1日 2月 3年")
    Integer dateType;

    private Long beginTime;

    private Long endTime;

    private String deviceId;
}
