package org.jetlinks.pro.sems.abutment.res;

import lombok.Data;

@Data
public class AlarmCountRes {
    private String totalCnt;

    private String remindCnt;

    private String warnCnt;

    private String noteCnt;
}
