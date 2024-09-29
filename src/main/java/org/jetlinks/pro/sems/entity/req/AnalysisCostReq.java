package org.jetlinks.pro.sems.entity.req;

import lombok.Data;
import org.jetlinks.pro.sems.entity.TestAreaEntity;

import java.util.List;

@Data
public class AnalysisCostReq {

    //选择的场所列表
    private List<TestAreaEntity> testAreaList;

    //x选择维度
    private Integer dimition;

    //开始时间
    private String startTime;

    //结束时间
    private String endTime;



}
