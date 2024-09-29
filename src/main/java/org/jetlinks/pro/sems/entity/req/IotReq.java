package org.jetlinks.pro.sems.entity.req;

import lombok.Data;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;

@Data
public class IotReq {

    QueryParamEntity query;
    String energyType;
}
