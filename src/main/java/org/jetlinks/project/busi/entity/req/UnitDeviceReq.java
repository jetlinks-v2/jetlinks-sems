package org.jetlinks.project.busi.entity.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;


@Data
public class UnitDeviceReq {

    @Schema(description = "能源类型 0-水，1-电，2-气")
    private String type;
    
    private String style;

    private QueryParamEntity query;
}
