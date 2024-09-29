package org.jetlinks.pro.sems.entity.req;

import lombok.Data;
import org.jetlinks.pro.sems.entity.EnergyRatioEntity;
import org.jetlinks.pro.sems.entity.OperateLogEntity;

import java.util.List;

@Data
public class EnergyRatioReq {

    private List<OperateLogEntity> operateLogList;

    private EnergyRatioEntity energyRatioEntity;
}
