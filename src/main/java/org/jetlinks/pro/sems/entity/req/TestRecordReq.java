package org.jetlinks.pro.sems.entity.req;

import lombok.Data;
import org.jetlinks.pro.sems.entity.TestEnergyDetailEntity;
import org.jetlinks.pro.sems.entity.TestRecordEntity;

import java.util.List;

@Data
public class TestRecordReq {

    private TestRecordEntity testRecordEntity;
    private List<TestEnergyDetailEntity> energyDetailList;

}