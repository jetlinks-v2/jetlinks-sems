package org.jetlinks.project.busi.strategy.peak.common;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.pro.IntervalUnit;
import org.jetlinks.project.busi.entity.res.PeakAnalysisInfo;
import org.jetlinks.project.busi.entity.res.PeakAnalysisRes;
import org.jetlinks.project.busi.enums.TimeEnum;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @ClassName TimeHandler
 * @Author hky
 * @Time 2023/7/13 18:22
 * @Description 时间处理类
 **/
public class TimeHandler {

    /**
     * 转换时间戳到日期级别
     * @param timestamp 时间戳
     * @return 转换后的时间戳
     */
    public static long dateChangeByYearAndTimeStep(Long timestamp) {
        LocalDate date = Instant.ofEpochMilli(timestamp)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
        return date.atStartOfDay(ZoneId.systemDefault())
                   .toInstant()
                   .toEpochMilli();

    }


    /**
     * 转换时间戳到分钟级别
     * @param timestamp 时间戳
     * @return 转换后的时间戳
     */
    public static long dateChangeByDayAndTimeStep(Long timestamp,Long endDate) {
        //当前时间对应的小时
//        long nowTime = System.currentTimeMillis();
        LocalDateTime nowDateTime = Instant.ofEpochMilli(endDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
        LocalDateTime dateTime = Instant.ofEpochMilli(timestamp)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TimeEnum.DAY.getDateFormatter());
        String dateTimeStr = dateTime.format(formatter);
        dateTime = LocalDateTime.parse(dateTimeStr, formatter);
        //当前时间对应的小时LocalDateTime
        String nowDateTimeStr = nowDateTime.format(formatter);
        LocalDateTime nowNewDateTime = LocalDateTime.parse(nowDateTimeStr, formatter);
        LocalDateTime localDateTime;
        if(nowNewDateTime.compareTo(dateTime)==0){
            localDateTime=nowDateTime;
        }else {
            localDateTime = dateTime.plusHours(1L);
        }

        return localDateTime.atZone(ZoneId.systemDefault())
                       .toInstant()
                       .toEpochMilli();

    }

    /**
     * 转换时间戳到月份级别
     * @param timestamp 时间戳
     * @return 转换后的时间戳
     */
    public static long dateChangeByMonthAndTimeStep(Long timestamp) {
        LocalDate dateTime = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM").parseDefaulting(ChronoField.DAY_OF_MONTH, 1).toFormatter();
        String dateTimeStr = dateTime.format(formatter);
        dateTime = LocalDate.parse(dateTimeStr, formatter);
        return dateTime.atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
    }

    /**
     * 根据查询时间段生成时间轴
     * @param query 前端参数
     * @param type 根据标识如何生成
     * @param res 返回数据
     * @return
     */
    public static PeakAnalysisRes generateTimes(QueryParamEntity query, String type, PeakAnalysisRes res) {
        String[] split = query
            .getTerms()
            .stream()
            .filter(term -> "btw".equals(term.getTermType()))
            .findFirst()
            .map(Term::getValue)
            .map(Object::toString)
            .orElseGet(() -> {
                LocalDate currentDate = LocalDate.now();
                LocalDateTime startOfDay = LocalDateTime.of(currentDate, LocalTime.MIN);
                LocalDateTime endOfDay = LocalDateTime.of(currentDate, LocalTime.MAX);
                long startOfDayTimestamp = startOfDay.toEpochSecond(ZoneOffset.UTC);
                long endOfDayTimestamp = endOfDay.toEpochSecond(ZoneOffset.UTC);
                return startOfDayTimestamp + "," + endOfDayTimestamp;
            })
            .split(",");

        List<Long> generateTimeSeries = generateTimeSeries(Long.parseLong(split[0]), Long.parseLong(split[1]),
                                                           1, TimeEnum.DAY
                                                               .name()
                                                               .equals(type) ? IntervalUnit.HOURS : TimeEnum.YEAR
                .name()
                .equals(type)? IntervalUnit.DAYS:IntervalUnit.MONTHS);
     return    Optional
            .ofNullable(res.getThread())
        .map(thread -> {
            for (Long timestamp : generateTimeSeries) {
                thread
                    .stream()
                    .filter(info -> info.getTime().equals(timestamp))
                    .findFirst()
                    .map(value -> thread)
                    .orElseGet(() -> {
                        thread.add(PeakAnalysisInfo.builder()
                                                   .time(timestamp)
                                                   .value(BigDecimal.ZERO)
                                                   .build());
                        return thread;
                    });
            }
            List<PeakAnalysisInfo> collect = thread.stream()
                                                   .sorted(Comparator.comparing(PeakAnalysisInfo::getTime))
                                                   .collect(Collectors.toList());
            res.setThread(collect);
            return res;
        })
            .orElseGet(() -> {
                List<PeakAnalysisInfo> thread = new ArrayList<>();
                for (Long timestamp : generateTimeSeries) {

                    thread.add( PeakAnalysisInfo.builder()
                                                .time(timestamp)
                                                .value(BigDecimal.ZERO)
                                                .build());

                }
                res.setThread(thread);
                return res;
            });


    }


    /**
     * 生成时间轴
     * @param startTimeStamp 开始时间戳
     * @param endTimeStamp 结束时间戳
     * @param intervalAmount 更新尺度
     * @param intervalUnit 更新维度
     * @return
     */
    public static List<Long> generateTimeSeries(long startTimeStamp, long endTimeStamp,
                                                long intervalAmount, IntervalUnit intervalUnit) {
        //获取当前时刻的小时
        long currentHours=System.currentTimeMillis()- System.currentTimeMillis()%3600000; //毫秒级
        LocalDateTime currentHoursDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentHours),
            ZoneId.systemDefault());
        List<LocalDateTime> timeSeries = new ArrayList<>();
        LocalDateTime current = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimeStamp),
            ZoneId.systemDefault());

        while (current.isBefore(LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimeStamp),
            ZoneId.systemDefault()))
            || current.equals(LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimeStamp),
            ZoneId.systemDefault()))) {
            timeSeries.add(current);
            if(current.compareTo(currentHoursDateTime)==0){
                current=LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimeStamp),
                    ZoneId.systemDefault());
            }else {
                current = current.plus(intervalAmount, intervalUnit.getUnit());
            }

        }

        return timeSeries
            .stream()
            .map(time -> time.atZone(ZoneId.systemDefault())
                             .toInstant()
                             .toEpochMilli())
            .collect(Collectors.toList());
    }


}
