package org.jetlinks.project.busi.entity.req;

import lombok.Data;
import org.jetlinks.project.busi.entity.res.ReturnTypeRes;
import org.jetlinks.project.busi.enums.EnergyDataBaseNameEnum;

import java.util.List;

@Data
public class EnergyAnalysisReq {
    /**按什么类型统计，1按实验，2按区域*/
    private String type;
    //区域或者实验id
   private List<ReturnTypeRes> nameAndId;

   /**能源类型对应的数据库名称*/
   private EnergyDataBaseNameEnum energyDataBaseNameEnum;

   /**搜索条件开始时间*/
   private Long startDate;

   /**搜索条件结束时间*/
   private Long endDate;

   private Integer EnergyType;

}
