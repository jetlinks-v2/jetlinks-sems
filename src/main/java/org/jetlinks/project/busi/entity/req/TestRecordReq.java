package org.jetlinks.project.busi.entity.req;

import lombok.Data;
import org.jetlinks.project.busi.entity.TestEnergyDetailEntity;
import org.jetlinks.project.busi.entity.TestRecordEntity;

import java.util.List;

@Data
public class TestRecordReq {

    private TestRecordEntity testRecordEntity;
    private List<TestEnergyDetailEntity> energyDetailList;

}