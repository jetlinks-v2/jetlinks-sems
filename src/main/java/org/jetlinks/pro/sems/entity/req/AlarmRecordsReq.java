package org.jetlinks.pro.sems.entity.req;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class AlarmRecordsReq {

    @NotBlank(message = "id不能为空")
    String ids;

    @NotBlank(message = "处理结果不能为空")
    String disposeResult;
}
