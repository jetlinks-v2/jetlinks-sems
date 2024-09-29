package org.jetlinks.pro.sems.entity.res;

import lombok.Data;

import java.util.List;

@Data
public class EnergyAnalysisRes {


    private String month;

    private List<EnergyChartRes> energyChartRes;
}
