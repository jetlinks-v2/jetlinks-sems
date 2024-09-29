package org.jetlinks.project.busi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.project.busi.strategy.peak.common.TimeHandler;

import java.sql.Time;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * @ClassName TimeEnum
 * @Author hky
 * @Time 2023/7/13 15:59
 * @Description
 **/
@Getter
@AllArgsConstructor
public enum TimeEnum {

    DAY("yyyy-MM-dd HH"),
    YEAR("yyyy-MM-dd"),
    MAXYEAR("YYYY-MM");

    private final String dateFormatter;


    public static TimeEnum getQueryDateType(QueryParamEntity query) {
        String[] split = query
            .getTerms()
            .stream()
            .filter(term -> "btw".equals(term.getTermType()))
            .findFirst()
            .map(Term::getValue)
            .map(Object::toString)
            .orElseGet(() -> System.currentTimeMillis() + "," + System.currentTimeMillis())
            .split(",");
        long statTime = TimeHandler.dateChangeByYearAndTimeStep(Long.parseLong(split[0]));
        long endTime = TimeHandler.dateChangeByYearAndTimeStep(Long.parseLong(split[1]));
        //判断搜索范围
        //如果小于7天，用小时
        //因为前端搜索是1号到8号，应该是包含8号的，所以这里默认加一天
        long day=(endTime-statTime)/(60*60*24*1000)+1;

        if (day<=7){
            return TimeEnum.DAY;
        }else if( day<=365){
            return TimeEnum.YEAR;
        }
        return TimeEnum.MAXYEAR;

    }






}
