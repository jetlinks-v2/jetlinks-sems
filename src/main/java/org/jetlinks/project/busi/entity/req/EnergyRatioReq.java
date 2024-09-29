package org.jetlinks.project.busi.entity.req;

import lombok.Data;
import org.jetlinks.project.busi.entity.EnergyRatioEntity;
import org.jetlinks.project.busi.entity.OperateLogEntity;

import java.util.List;

@Data
public class EnergyRatioReq {

    private List<OperateLogEntity> operateLogList;

    private EnergyRatioEntity energyRatioEntity;
}
