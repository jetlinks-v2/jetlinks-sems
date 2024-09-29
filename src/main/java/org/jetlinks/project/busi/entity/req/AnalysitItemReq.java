package org.jetlinks.project.busi.entity.req;

import lombok.Data;
import org.jetlinks.project.busi.entity.TestConfigEntity;

import java.util.List;

@Data
public class AnalysitItemReq {
    //选择的场所列表
    private List<TestConfigEntity> itemList;

    //开始时间
    private String startTime;

    //结束时间
    private String endTime;

}
