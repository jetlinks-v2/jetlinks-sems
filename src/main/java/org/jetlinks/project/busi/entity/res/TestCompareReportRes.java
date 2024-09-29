package org.jetlinks.project.busi.entity.res;

import lombok.Data;

import java.util.List;

@Data
public class TestCompareReportRes {

    List<TestAnalysisReportRes> list;

    private String areaName;

    private List<String> remark;

}
