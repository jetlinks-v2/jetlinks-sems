package org.jetlinks.project.busi.entity.req;

import lombok.Data;
import org.jetlinks.project.busi.entity.HitDotEntity;

import java.util.List;

@Data
public class HitDotReq {

    /**
     * 楼层
     * */
    private String floor;

    private List<HitDotEntity> hitDotList;
}
