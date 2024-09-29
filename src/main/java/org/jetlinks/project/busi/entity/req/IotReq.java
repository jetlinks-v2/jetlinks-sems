package org.jetlinks.project.busi.entity.req;

import lombok.Data;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;

@Data
public class IotReq {

    QueryParamEntity query;
    String energyType;
}
