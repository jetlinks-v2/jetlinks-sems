package org.jetlinks.project.busi.utils;

import org.springframework.util.ObjectUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * lqd
 * 日期工具类
 */
public class DateUtil {
    /**
     * 简单年日期格式
     */
    public static final String DATE_YEAR = "yyyy";
    /**
     * 简单月日期格式
     */
    public static final String DATE_MONTH = "MM";
    /**
     * 简单日日期格式
     */
    public static final String DATE_DAY = "dd";
    /**
     * 简单周日期格式
     */
    public static final String DATE_WEEK = "EE";

    /**
     * 简单年月日期格式
     */
    public static final String DATE_SHORT_SIMPLE_MONTH_FORMAT = "yyyyMM";
    /**
     * 简单年月日期格式
     */
    public static final String DATE_SHORT_SIMPLE_MONTH_FORMAT_NEW = "yyMM";
    /**
     * 简单年月日日期格式
     */
    public static final String DATE_SHORT_SIMPLE_FORMAT = "yyyyMMdd";
    /**
     * 简单年月日日期格式
     */
    public static final String DATE_SHORT_YEAR_SIMPLE_FORMAT = "yyMMdd";
    /**
     * 简单年月日 时格式
     */
    public static final String DATE_SHORT_SIMPLE_FORMAT_WITHHOUR = "yyyyMMddHH";
    /**
     * 简单年月日 时 分格式
     */
    public static final String DATE_SHORT_SIMPLE_FORMAT_WITHMINUTE = "yyyyMMddHHmm";
    /**
     * 年月日时分秒格式
     */
    public static final String DATE_LONG_SMAIL_FORMAT = "yyMMddHHmmss";
    /**
     * 年月日时分秒格式
     */
    public static final String DATE_LONG_SIMPLE_FORMAT = "yyyyMMddHHmmss";
    /**
     * 简单时分秒毫秒
     */
    public static final String DATE_SHORT_TIME_FORMAT = "HHmmss.S";
    /**
     * 简单时分秒毫秒
     */
    public static final String DATE_SHORT_TIME_FORMATS = "HHmmssS";
    /**
     * 简单时分钞
     **/
    public static final String DATE_TIME_FORMAT = "HHmmss";
    /**
     * 年月日日期格式
     */
    public static final String DATE_SHORT_FORMAT = "yyyy-MM-dd";
    /**
     * 年月日期格式
     */
    public static final String DATE_SHORT_YEAR_MONTH = "yyyy-MM";
    /**
     * 中文年月日日期格式
     */
    public static final String DATE_SHORT_CHN_FORMAT = "yyyy年MM月dd日";
    /**
     * 年月日时日期格式
     */
    public static final String DATE_WITHHOUR_FORMAT = "yyyy-MM-dd HH";
    /**
     * 中文年月日时日期格式
     */
    public static final String DATE_WITHHOUR_CHN_FORMAT = "yyyy年MM月dd日 HH";
    /**
     * 年月日时分日期格式
     */
    public static final String DATE_WITHMINUTE_FORMAT = "yyyy-MM-dd HH:mm";
    /**
     * 中文年月日时分日期格式
     */
    public static final String DATE_WITHMINUTE_CHN_FORMAT = "yyyy年MM月dd日 HH:mm";
    /**
     * 年月日时分秒日期格式
     */
    public static final String DATE_WITHSECOND_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final String DATE_WITHSECOND = "yyyy-MM-dd HH:00:00";

    /**
     * 中文年月日时分秒日期格式
     */
    public static final String DATE_WITHSECOND_CHN_FORMAT = "yyyy年MM月dd日 HH:mm:ss";
    /**
     * 年月日时分秒毫秒日期格式
     */
    public static final String DATE_WITHMILLISECOND_FORMAT = "yyyy-MM-dd HH:mm:ss.S";
    /**
     * 中文年月日时分秒毫秒日期格式
     */
    public static final String DATE_WITHMILLISECOND_CHN_FORMAT = "yyyy年MM月dd日 HH:mm:ss.S";
    /**
     * 格林时间格式
     */
    public static final String DATE_UTC_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /**
     * 将给定应用服务器日期按照给定格式化类型转换成字符串
     *
     * @param date   -java日期对象
     * @param format -日期格式化类型
     * @return String -返回转换后的字符串
     */
    public static String dateToString(Date date, String format) {
        if (ObjectUtils.isEmpty(date)) {
            return null;
        }

        if (ObjectUtils.isEmpty(format)) {
            format = DATE_WITHSECOND_FORMAT;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    /**
     * 将给定应用服务器日期按照默认格式化(yyyy-MM-dd HH:mm:ss)类型转换成字符串
     *
     * @param date -java日期对象
     * @return String -返回转换后的字符串
     */
    public static String dateToString(Date date) {
        return dateToString(date, DATE_WITHSECOND_FORMAT);
    }

    /**
     * 将给定应用服务器日期按照默认格式化(yyyy-MM-dd)类型转换成字符串
     *
     * @param date -java日期对象
     * @return String -返回转换后的字符串
     */
    public static String dateToStringYMD(Date date) {
        return dateToString(date, DATE_SHORT_FORMAT);
    }

    /**
     * 将给定应用服务器日期按照默认格式化(HH)类型转换成字符串
     *
     * @param date -java日期对象
     * @return String -返回转换后的字符串
     */
    public static String dateToStringH(Date date) {
        return dateToString(date, "HH");
    }

    /**
     * 将给定应用服务器日期按照默认格式化(HHmmss.S)类型转换成字符串
     *
     * @param date -java日期对象
     * @return String -返回转换后的字符串
     */
    public static String shortTimeToString(Date date) {
        return dateToString(date, DATE_SHORT_TIME_FORMAT);
    }

    /**
     * 将给定应用服务器日期按照默认格式化(yyyyMMdd)类型转换成字符串
     *
     * @param date -java日期对象
     * @return String -返回转换后的字符串
     */
    public static String shortDateToString(Date date) {
        return dateToString(date, DATE_SHORT_SIMPLE_FORMAT);
    }

    /**
     * 将应用服务器当前日期按照给定格式化类型转换成字符串
     *
     * @param format -日期格式化类型
     * @return String -返回转换后的字符串
     */
    public static String currentTimeToString(String format) {
        return dateToString(getCurrentDateTime(), format);
    }

    /**
     * 将字符串转换成日期 注意：一定要选用匹配的格式，否则不能解析，将返回null
     *
     * @param strDate - 日期
     * @param format  - 格式
     * @return Date -转换后的日期
     */
    public static Date stringToDate(String strDate, String format) {
        if (ObjectUtils.isEmpty(strDate)) {
            return null;
        }
        if (ObjectUtils.isEmpty(format)) {
            format = DATE_SHORT_FORMAT;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            return sdf.parse(strDate);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 将字符串转换成日期，默认格式：yyyy-MM-dd
     *
     * @param strDate - 日期
     * @return Date -转换后的日期
     */
    public static Date stringToDate(String strDate) {
        if (ObjectUtils.isEmpty(strDate)) {
            return null;
        }
        return stringToDate(strDate, DATE_SHORT_FORMAT);
    }

    /**
     * 将字符串转换成日期，默认格式：yyyy-MM-dd
     *
     * @param strDate - 日期
     * @return Date -转换后的日期
     */
    public static Date stringToDateYMDHMS(String strDate) {
        if (ObjectUtils.isEmpty(strDate)) {
            return null;
        }
        return stringToDate(strDate, DATE_WITHSECOND_FORMAT);
    }

    /**
     * 获取当前日期
     *
     * @return Date -转换后的日期
     */
    public static Date getCurrentDateTime() {
        Calendar c = Calendar.getInstance();
        return c.getTime();
    }

    public static Calendar StingToCalendar(String date) throws ParseException {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date parse = sdf.parse(date);
        cal.setTime(parse);
        return cal;
    }

    /**
     * 获取当天开始时间
     *
     * @return java.util.Date
     * @author lqd
     * @date 2020/9/2
     */
    public static Date getTodayStartTime() {
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        return todayStart.getTime();
    }

    /**
     * 获取当天结束时间
     *
     * @return java.util.Date
     * @author lqd
     * @date 2020/9/2
     */
    public static Date getTodayEndTime() {
        Calendar todayEnd = Calendar.getInstance();
        todayEnd.set(Calendar.HOUR_OF_DAY, 23);
        todayEnd.set(Calendar.MINUTE, 59);
        todayEnd.set(Calendar.SECOND, 59);
        return todayEnd.getTime();
    }

    /**
     * 获取本周一0点时间
     *
     * @return java.util.Date
     * @author lqd
     * @date 2020/9/2
     */
    public static Date getTimesWeekmorning() {
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONDAY), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return cal.getTime();
    }

    /**
     * 获得本周日24点时间
     *
     * @return java.util.Date
     * @author lqd
     * @date 2020/9/2
     */
    public static Date getTimesWeeknight() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getTimesWeekmorning());
        cal.add(Calendar.DAY_OF_WEEK, 7);
        return cal.getTime();
    }

    /**
     * 获取本月第一天0点时间
     *
     * @return java.util.Date
     * @author lqd
     * @date 2020/9/2
     */
    public static Date getTimesMonthMorning() {
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONDAY), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        return cal.getTime();
    }

    /**
     * 获取本月的最后一天最后时刻时间
     *
     */

    public static Date getLastDayOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONDAY), cal.getMinimum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        cal.add(Calendar.DAY_OF_YEAR, getCurrentMonthDay() - 1);
        return cal.getTime();
    }
    /**
     * 获取前30天的时间
     *
     */
    public static Date getLast30Day(){
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH,-31);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTime();
    }

    /**
     * 获取本月天数
     *
     * @return java.util.Date
     * @author lqd
     * @date 2020/9/2
     */
    public static int getCurrentMonthDay() {
        Calendar a = Calendar.getInstance();
        a.set(Calendar.DATE, 1);
        a.roll(Calendar.DATE, -1);
        int maxDate = a.get(Calendar.DATE);
        return maxDate;

    }

    /**
     * 获取某年的某月天数
     *
     * @return java.util.Date
     * @author lqd
     * @date 2022/12/16
     */
    public static int getCurrentMonthDay(int year,int month) {
        Calendar c = Calendar.getInstance();
        c.set(year,month,1);    //1为1日
        //java月份从0开始，输入的月份比实际得到的月+1，即month值+1月1日
        //如，输入的是3月，输出的为4月
        c.add(Calendar.DATE,-1);    //-1为减1天，即month值+1月1的前一天，此时可得到想要的正确的月份
        return c.get(Calendar.DATE);
    }

    // 计算两个日期之间的差值
    public static long getDateDiff(Date startDate, Date endDate, TimeUnit timeUnit) {
        long diffInMillis = endDate.getTime() - startDate.getTime();
        return timeUnit.convert(diffInMillis, TimeUnit.MILLISECONDS);
    }



    /**
     * 获取下月第一天0点时间
     *
     * @return java.util.Date
     * @author lqd
     * @date 2020/9/2
     */
    public static Date getTimesMonthnight() {
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONDAY), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 24);
        return cal.getTime();
    }
    /**
     * 获取上月第一天开始时间
     *
     * @return java.util.Date
     * @author lqd
     * @date 2020/9/2
     */
    public static Date getLastMonthBegin() {
        Calendar lastMonthFirst = Calendar.getInstance();
        lastMonthFirst.add(Calendar.MONTH, -1);  // 上月
        lastMonthFirst.set(Calendar.DAY_OF_MONTH, 1);  // 上月第一天
        lastMonthFirst.set(Calendar.HOUR_OF_DAY, 0);
        lastMonthFirst.set(Calendar.MINUTE, 0);
        lastMonthFirst.set(Calendar.SECOND, 0);
        lastMonthFirst.set(Calendar.MILLISECOND, 0);
        return lastMonthFirst.getTime();
    }
    /**
     * 获取上月最后一天结束时间
     *
     * @return java.util.Date
     * @author lqd
     * @date 2020/9/2
     */
    public static Date getLastMonthEnd() {
        Calendar lastMonthLast = Calendar.getInstance();
        lastMonthLast.add(Calendar.MONTH, -1);   // 上月
        lastMonthLast.set(Calendar.DAY_OF_MONTH, lastMonthLast.getActualMaximum(Calendar.DAY_OF_MONTH));
        lastMonthLast.set(Calendar.HOUR_OF_DAY, 23);
        lastMonthLast.set(Calendar.MINUTE, 59);
        lastMonthLast.set(Calendar.SECOND, 59);
        lastMonthLast.set(Calendar.MILLISECOND, 999);

        return lastMonthLast.getTime();
    }

    /**
     * 输入当前月份月+day月份的天数,(可以使用本方法获取上月天数，本月天数，)
     *
     * @return java.util.Date
     * @author lqd
     * @date 2020/9/2
     */
    public static Integer LastMonthDays(int day) {
        Calendar lastMonth = Calendar.getInstance();
        lastMonth.add(Calendar.MONTH, day);

        return lastMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * 获取去年日期第一天
     *
     * @return java.util.Date
     * @author lqd
     * @date 2020/9/2
     */
    public static Date getLastYearBegin() {
        Calendar lastYear = Calendar.getInstance();
        lastYear.add(Calendar.YEAR, -1);
        lastYear.set(Calendar.MONTH, 0);
        lastYear.set(Calendar.DATE, 1);
        lastYear.set(Calendar.HOUR_OF_DAY, 0);
        lastYear.set(Calendar.MINUTE, 0);
        lastYear.set(Calendar.SECOND, 0);
        lastYear.set(Calendar.MILLISECOND, 0);
        return lastYear.getTime();
    }

    /**
     * 获取去年日期最后一天
     *
     * @return java.util.Date
     * @author lqd
     * @date 2020/9/2
     */
    public static Date getLastYearEnd() {
        Calendar lastYear = Calendar.getInstance();
        lastYear.add(Calendar.YEAR, -1);
        lastYear.set(Calendar.MONTH, 11);
        lastYear.set(Calendar.DATE, 31);
        lastYear.set(Calendar.HOUR_OF_DAY, 23);
        lastYear.set(Calendar.MINUTE, 59);
        lastYear.set(Calendar.SECOND, 59);
        lastYear.set(Calendar.MILLISECOND, 999);
        return lastYear.getTime();
    }

    /**
     * 获取今年日期第一天
     *
     * @return java.util.Date
     * @author lqd
     * @date 2020/9/2
     */
    public static String getYearBegin() {
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), 0, cal.get(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));

        return dateToStringYMD(cal.getTime());
    }

    /**
     * 获取今年日期最后一天
     *
     * @return java.util.Date
     * @author lqd
     * @date 2020/9/2
     */
    public static String getYearEnd() {
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), 11, cal.get(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));

        return dateToStringYMD(cal.getTime());
    }

    /**
     * 获取当年的第一天
     */
    public static Date getCurrentFirstOfYear(){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
        calendar.set(Calendar.MONTH, Calendar.JANUARY);  // 1月
        calendar.set(Calendar.DAY_OF_MONTH, 1);          // 1日
        calendar.set(Calendar.HOUR_OF_DAY, 0);           // 0时
        calendar.set(Calendar.MINUTE, 0);                // 0分
        calendar.set(Calendar.SECOND, 0);                // 0秒

        return calendar.getTime();
    }

    /**
     * 获取当年的最后一天最后一秒
     */
    public static Date getCurrentLastOfYear(){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
        calendar.set(Calendar.MONTH, Calendar.DECEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 31);  // 12月31日
        calendar.set(Calendar.HOUR_OF_DAY, 23);   // 23时
        calendar.set(Calendar.MINUTE, 59);        // 59分
        calendar.set(Calendar.SECOND, 59);        // 59秒
        return calendar.getTime();
    }


    /**
     * 获取当前时间，精确到秒（没转化格式）
     *
     * @return Date -转换后的日期
     */
    public static Date getCurrentTime() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * 获取当前年份-长
     *
     * @return String -当前年份
     */
    public static String getCurrentLongYear() {
        Calendar c = Calendar.getInstance();
        String year = Integer.toString(c.get(Calendar.YEAR));
        return year;
    }

    /**
     * 获取指定时间年份
     *
     * @param date
     * @return
     */
    public static int getYear(Date date) {
        Calendar c = Calendar.getInstance();
        if (!ObjectUtils.isEmpty(date)) {
            c.clear();
            c.setTime(date);
        }
        return c.get(Calendar.YEAR);
    }

    /**
     * 获取指定时间月份
     *
     * @param date
     * @return
     */
    public static int getMonth(Date date) {

        Calendar c = Calendar.getInstance();
        if (!ObjectUtils.isEmpty(date)) {
            c.clear();
            c.setTime(date);
        }
        return c.get(Calendar.MONTH) + 1;

    }

    /**
     * 传入日期加上年
     *
     * @param date
     * @param year
     * @return
     */
    public static String addYear(Date date, int year) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.YEAR, year);
        return dateToString(c.getTime(), DATE_SHORT_YEAR_MONTH);
    }

    /**
     * 传入日期加上年
     *
     * @param date
     * @param year
     * @return
     */
    public static String addYears(Date date, int year) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.YEAR, year);
        return dateToString(c.getTime(), DATE_WITHSECOND_FORMAT);
    }


    /**
     * 传入日期加上年
     *
     * @param date
     * @param year
     * @return
     */
    public static String addYear(Date date, int year, String formater) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.YEAR, year);
        return dateToString(c.getTime(), formater);
    }


    /**
     * 传入日期加上月份
     *
     * @param date
     * @param month
     * @return
     */
    public static String addMonth(Date date, int month, String formater) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.MONTH, month);
        return dateToString(c.getTime(), formater);
    }

    /**
     * 传入日期加上月份
     *
     * @param date
     * @param month
     * @return
     */
    public static Date addMonths(Date date, int month) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.MONTH, month);
        return c.getTime();
    }

    /**
     * 传入日期加上月份
     *
     * @param date
     * @param month
     * @return
     */
    public static String addMonth(Date date, int month) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.MONTH, month);
        return dateToString(c.getTime(), DATE_WITHSECOND_FORMAT);
    }

    /**
     * 取得传入时间的月份第一天
     *
     * @return
     */
    public static String monthFirst(Date date, String formater) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return dateToString(calendar.getTime(), DATE_SHORT_FORMAT);
    }

    public static Date monthLast(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        //获得实体类
        Calendar ca = Calendar.getInstance();
        ca.setTime(date);
        LocalDate localDate = LocalDate.of(getYear(date), getMonth(date), getDay(date));
        int i = localDate.lengthOfMonth();
        //设置最后一天
        ca.set(Calendar.DAY_OF_MONTH, i);
        //最后一天格式化
        String lastDay = format.format(ca.getTime());
        return stringToDate(lastDay, DATE_SHORT_FORMAT);
    }

    /**
     * 取得传入时间的月份第一天
     *
     * @return
     */
    public static String monthFirst() {
        return monthFirst(new Date(), DATE_SHORT_FORMAT);
    }

    /**
     * 取得传入时间的月份第一天
     *
     * @return
     */
    public static String monthFirst(String formater) {
        return monthFirst(new Date(), formater);
    }


    /**
     * 传入日期加上天
     *
     * @param date
     * @param day
     * @return
     */
    public static String addDay(Date date, int day) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DAY_OF_YEAR, day);
        return dateToString(c.getTime(), DATE_SHORT_FORMAT);
    }

    /**
     * 传入日期加上天
     *
     * @param date
     * @param day
     * @return
     */
    public static String addDay(String date, int day) {
        Calendar c = Calendar.getInstance();
        c.setTime(stringToDate(date, DATE_SHORT_FORMAT));
        c.add(Calendar.DAY_OF_YEAR, day);
        return dateToString(c.getTime(), DATE_SHORT_FORMAT);
    }

    /**
     * 传入日期加上天
     *
     * @param date    传入时间
     * @param day     天数
     * @param formate 日期格式
     * @return
     */
    public static String addDay(Date date, int day, String formate) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DAY_OF_YEAR, day);
        return dateToString(c.getTime(), formate);
    }

    /**
     * 传入日期加上天
     *
     * @param date
     * @param day
     * @return
     */
    public static Date addDays(Date date, int day) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DAY_OF_YEAR, day);
        return c.getTime();
    }

    /**
     * 传入日期加上天
     *
     * @param date
     * @param day
     * @return
     */
    public static Date addDays(Date date, int day, String formate) {
        String addDay = addDay(date, day, formate);
        return stringToDate(addDay);
    }

    public static Date addSecond(Date date, int second) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.SECOND, second);
        return c.getTime();
    }

    /**
     * 根据指定时间获取所在天数
     *
     * @param date
     * @return
     */
    public static int getDay(Date date) {
        Calendar c = Calendar.getInstance();
        if (!ObjectUtils.isEmpty(date)) {
            c.clear();
            c.setTime(date);
        }
        return c.get(Calendar.DATE);
    }

    /**
     * 根据指定时间获取所在天数
     *
     * @param newDate
     * @param date
     * @return
     */
    public static Date setDay(Date newDate, Date date) {
        Calendar c = Calendar.getInstance();
        if (!ObjectUtils.isEmpty(date)) {
            c.clear();
            c.setTime(date);
            c.set(getYear(newDate), getMonth(newDate), getDay(newDate));
        }
        return c.getTime();
    }

    /**
     * 传入的时间加上分钟数
     *
     * @param strDate
     * @param format
     * @param mu
     * @return
     */
    public static Date dateAdd(String strDate, String format, Integer mu) {
        Date par = stringToDate(strDate, format);
        Long time = (long) 1000 * 60 * mu;
        return new Date(par.getTime() + time);
    }

    /**
     * 添加分钟数
     *
     * @param date  时间
     * @param minue 添加分钟数
     * @return
     */
    public static Date addMinute(Date date, int minue) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.MINUTE, minue);
        return c.getTime();
    }

    /**
     * 添加小时
     *
     * @param date   时间
     * @param format 转换后的格式
     * @param minue  添加分钟数
     * @return
     */
    public static String addMinute(Date date, int minue, String format) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.MINUTE, minue);
        if (ObjectUtils.isEmpty(format)) {
            format = DATE_WITHSECOND_FORMAT;
        }
        return dateToString(c.getTime(), format);
    }

    /**
     * 添加小时
     *
     * @param date   时间
     * @param format 转换后的格式
     * @param hour   添加小时数
     * @return
     */
    public static String addHour(Date date, long hour, String format) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.HOUR, (int) hour);
        if (ObjectUtils.isEmpty(format)) {
            format = DATE_WITHSECOND_FORMAT;
        }
        return dateToString(c.getTime(), format);
    }

    /**
     * 传入时间添加小时
     *
     * @param date
     * @param hour
     * @return
     */
    public static Date addHour(Date date, long hour) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.HOUR, (int) hour);
        return c.getTime();
    }

    /**
     * 传入时间添加小时
     *
     * @param date
     * @param hour
     * @return
     */
    public static Date addHour(Date date, Double hour) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int min = 0;
        if (!ObjectUtils.isEmpty(hour)) {
            min = (int) (hour * 60);
        }
//		c.add(Calendar.HOUR, (int)hour);
        c.add(Calendar.MINUTE, min);
        return c.getTime();
    }

    /**
     * 传入时间添加年
     *
     * @param date
     * @param year
     * @return
     */
    public static Date addYearData(Date date, Integer year) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.YEAR, year);
        return c.getTime();
    }

    /**
     * 传入的时间加上分钟数
     *
     * @param strDate
     * @param mu
     * @param mu
     * @return
     */
    public static Date dateAdd(Date strDate, Integer mu) {
        Long time = (long) 1000 * 60 * mu;
        return new Date(strDate.getTime() + time);
    }

    /**
     * 传入时间减去分钟数
     *
     * @param strDate
     * @param format
     * @param mu
     * @return
     */
    public static Date dateReduce(String strDate, String format, Integer mu) {
        Date par = stringToDate(strDate, format);
        Long time = (long) 1000 * 60 * mu;
        return new Date(par.getTime() - time);

    }

    /**
     * 传入时间减去分钟数
     *
     * @param strDate
     * @param mu
     * @return
     */
    public static Date dateReduce(Date strDate, Integer mu) {
        Long time = (long) 1000 * 60 * mu;
        return new Date(strDate.getTime() - time);

    }

    /**
     * 传入时间减去分钟数
     *
     * @param strDate
     * @param mu
     * @return
     */
    public static Date dateReduce(Date strDate, double mu) {
        Long time = (long) (1000 * 60 * mu);
        return new Date(strDate.getTime() - time);

    }

    /**
     * 传入时间加上分钟数
     *
     * @param strDate
     * @param mu
     * @return
     */
    public static Date dateAdd(Date strDate, double mu) {
        Long time = (long) (1000 * 60 * mu);
        return new Date(strDate.getTime() + time);

    }

    /**
     * 获取指定时间的后一天的指定类型日期
     *
     * @param date
     * @param month
     * @return String
     */
    public static Date getAfterMonth(Date date, int month) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.setTime(date);
        c.add(Calendar.MONTH, month);
        return c.getTime();
    }

    /**
     * 获取指定时间的后一天的指定类型日期
     *
     * @param date
     * @param day
     * @return String
     */
    public static Date getAfterDay(Date date, int day) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.setTime(date);
        c.add(Calendar.DAY_OF_MONTH, day);
        return c.getTime();
    }

    /**
     * 获取指定时间的季度开始时间和结束时间
     *
     * @param date
     * @return
     */
    public static String[] getQuarterly(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        // 取得年份
        String year = String.valueOf(calendar.get(Calendar.YEAR));
        // 取得月份
        String month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
        String[] rtn = new String[3];
        switch (Integer.parseInt(month) / 3) {
            case 0:
                rtn[0] = year + "-01-01";
                rtn[1] = year + "-03-31";
                rtn[2] = "1";
                break;
            case 1:
                rtn[0] = year + "-04-01";
                rtn[1] = year + "-06-31";
                rtn[2] = "2";
                break;
            case 2:
                rtn[0] = year + "-07-01";
                rtn[1] = year + "-09-30";
                rtn[2] = "3";
                break;
            case 3:
                rtn[0] = year + "-10-01";
                rtn[1] = year + "-12-31";
                rtn[2] = "3";
                break;
            default:
                rtn[0] = year + "-01-01";
                rtn[1] = year + "-03-31";
                rtn[2] = "1";
                break;
        }
        return rtn;
    }

    /**
     * 获取指定时间的季度开始时间和结束时间
     *
     * @param date
     * @return
     */
    public static String[] getQuarterly(String date) {
        String[] rtn = new String[3];
        String year = date.substring(0, 4);
        String month = "";
        if (date.length() == 8) {
            month = date.substring(4, 6);
        } else {
            month = date.substring(5, 7);
        }
        switch (Integer.parseInt(month) / 3) {
            case 0:
                rtn[0] = year + "-01-01";
                rtn[1] = year + "-03-31";
                rtn[2] = "1";
                break;
            case 1:
                rtn[0] = year + "-04-01";
                rtn[1] = year + "-06-31";
                rtn[2] = "2";
                break;
            case 2:
                rtn[0] = year + "-07-01";
                rtn[1] = year + "-09-30";
                rtn[2] = "3";
                break;
            case 3:
                rtn[0] = year + "-10-01";
                rtn[1] = year + "-12-31";
                rtn[2] = "3";
                break;
            default:
                rtn[0] = year + "-01-01";
                rtn[1] = year + "-03-31";
                rtn[2] = "1";
                break;
        }
        return rtn;
    }



    /**
     * 获取当年的开始时间戳
     *
     * @param timeStamp 毫秒级时间戳
     * @param timeZone  如 GMT+8:00
     * @return
     */
    public static Long getYearStartTime(Long timeStamp, String timeZone) {
        Calendar calendar = Calendar.getInstance();// 获取当前日期
        calendar.setTimeZone(TimeZone.getTimeZone(timeZone));
        calendar.setTimeInMillis(timeStamp);
        calendar.add(Calendar.YEAR, 0);
        calendar.add(Calendar.DATE, 0);
        calendar.add(Calendar.MONTH, 0);
        calendar.set(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取当年的最后时间戳
     *
     * @param timeStamp 毫秒级时间戳
     * @param timeZone  如 GMT+8:00
     * @return
     */
    public static Long getYearEndTime(Long timeStamp, String timeZone) {
        Calendar calendar = Calendar.getInstance();// 获取当前日期
        calendar.setTimeZone(TimeZone.getTimeZone(timeZone));
        calendar.setTimeInMillis(timeStamp);
        int year = calendar.get(Calendar.YEAR);
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        calendar.roll(Calendar.DAY_OF_YEAR, -1);
        return calendar.getTimeInMillis();
    }


    /**
     * 获取当月开始时间戳
     *
     * @param timeStamp 毫秒级时间戳
     * @param timeZone  如 GMT+8:00
     * @return
     */
    public static Long getMonthStartTime(Long timeStamp, String timeZone) {
        Calendar calendar = Calendar.getInstance();// 获取当前日期
        calendar.setTimeZone(TimeZone.getTimeZone(timeZone));
        calendar.setTimeInMillis(timeStamp);
        calendar.add(Calendar.YEAR, 0);
        calendar.add(Calendar.MONTH, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 1);// 设置为1号,当前日期既为本月第一天
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取当月的结束时间戳
     *
     * @param timeStamp 毫秒级时间戳
     * @param timeZone  如 GMT+8:00
     * @return
     */
    public static Long getMonthEndTime(Long timeStamp, String timeZone) {
        Calendar calendar = Calendar.getInstance();// 获取当前日期
        calendar.setTimeZone(TimeZone.getTimeZone(timeZone));
        calendar.setTimeInMillis(timeStamp);
        calendar.add(Calendar.YEAR, 0);
        calendar.add(Calendar.MONTH, 0);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));// 获取当前月最后一天
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取指定某一天的开始时间戳
     *
     * @param timeStamp 毫秒级时间戳
     * @param timeZone  如 GMT+8:00
     * @return
     */
    public static Long getDailyStartTime(Long timeStamp, String timeZone) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone(timeZone));
        calendar.setTimeInMillis(timeStamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取指定某一天的结束时间戳
     *
     * @param timeStamp 毫秒级时间戳
     * @param timeZone  如 GMT+8:00
     * @return
     */
    public static Long getDailyEndTime(Long timeStamp, String timeZone) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone(timeZone));
        calendar.setTimeInMillis(timeStamp);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }


    /**
     * 获取指定某周的开始时间戳
     * @return
     */
    public static Long getWeekStartTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        //start of the week
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_YEAR,-1);
        }
        calendar.add(Calendar.DAY_OF_WEEK, -(calendar.get(Calendar.DAY_OF_WEEK) - 2));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Long startTime = calendar.getTimeInMillis();
        return startTime;
    }

    /**
     * 获取指定某周的结束时间戳
     * @return
     */
    public static Long getWeekEndTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        // Adjust Calendar to the next week, then look for the start time of that week
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        } else {
            calendar.add(Calendar.DAY_OF_WEEK, -(calendar.get(Calendar.DAY_OF_WEEK) - 2) + 7);
        }

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Subtract 1 millisecond to get the end time of the current week.
        Long endTime = calendar.getTimeInMillis() - 1;

        return endTime;
    }

    /**startDate和endDate之间的时间列表
     *<pre>{@code
     *
     *}</pre>
     * @param
     * @return
     * @see
     */
    public static List<String> getHoursBetweenDates(String dimension, LocalDateTime startDate, LocalDateTime endDate) {
        //获取当前时刻的小时
        List<String> result = new ArrayList<>();
        DateTimeFormatter formatter;
        if(dimension.equals("%Y-%m-%d %H:00")){
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");
            LocalDateTime tempDateTime = LocalDateTime.from(startDate);
            while (!tempDateTime.isAfter(endDate)) {
                result.add(tempDateTime.format(formatter));
                tempDateTime = tempDateTime.plusHours(1);
            }
            //添加当前时刻
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                tempDateTime=endDate;
                result.add(tempDateTime.format(formatter));
        }else if(dimension.equals("%Y-%m-%d")){
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDateTime tempDateTime = LocalDateTime.from(startDate);
            while (!tempDateTime.isAfter(endDate)) {
                result.add(tempDateTime.format(formatter));
                tempDateTime = tempDateTime.plusDays(1);
            }
        }else{
            formatter = DateTimeFormatter.ofPattern("yyyy-MM");
            LocalDateTime tempDateTime = LocalDateTime.from(startDate);
            while (!tempDateTime.isAfter(endDate)) {
                result.add(tempDateTime.format(formatter));
                tempDateTime = tempDateTime.plusMonths(1);
            }
            if(tempDateTime.getMonth().getValue()==endDate.getMonth().getValue() &&
               tempDateTime.getDayOfMonth()> endDate.getDayOfMonth())
            result.add(tempDateTime.format(formatter));
        }
        return result;
    }
    /**获取上个月的今天时间戳
    *<pre>{@code
    *
    *}</pre>
    * @param
    * @return
    * @see
    */
    public static long getLastMonthCurrent(){
        // 设置时区为中国
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Shanghai");
        Calendar calendar = Calendar.getInstance(timeZone);

        // 获取当前日期
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // 将日期设置为上个月的今天
        calendar.set(year, month - 1, day);
        // 获取时间戳（13位）
        return calendar.getTimeInMillis();
    }


    /**
     * 取得传入时间的月份的最大天数
     *
     * @param date
     * @return
     */
    public static int getActualMaximum(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.getActualMaximum(Calendar.DATE);
    }

    /**
     * 取得传入时间的月份的最大天数
     *
     * @param date
     * @return
     */
    public static int getActualMaximum(String date) {
        return getActualMaximum(stringToDate(date));
    }

    /**
     * 取得传入时间的月份的最大天数
     *
     * @param date
     * @return
     */
    public static int getActualMaximum(String date, String fomatter) {
        return getActualMaximum(stringToDate(date, fomatter));
    }

    /**
     * 报表类型取得时间
     *
     * @param date 时间
     * @param type 类型
     * @return
     */
    public static Date[] reportDate(String date, String type) {
        if (ObjectUtils.isEmpty(date)) {
            date = dateToString(new Date(), DATE_SHORT_FORMAT);
        }

        Date startDate = null;
        Date endDate = null;
        // 判断报表类型
        if ("year".equals(type)) {
            startDate = stringToDate(
                    date.substring(0, 4) + "-01-01",
                    DATE_SHORT_FORMAT);
            endDate = stringToDate(
                    (Integer.parseInt(date.substring(0, 4)) + 1) + "-01-01",
                    DATE_SHORT_FORMAT);
        } else if ("month".equals(type)) {
            startDate = stringToDate(date.substring(0, 7) + "-01",
                    DATE_SHORT_FORMAT);
            endDate = getAfterMonth(startDate, 1);
        } else if ("day".equals(type)) {
            startDate = stringToDate(date,
                    DATE_SHORT_FORMAT);
            endDate = getAfterDay(startDate, 1);
        }

        return new Date[]{startDate, endDate};
    }

    /**
     * 取得传入时间的相差的月数
     *
     * @param startDate 开始
     * @param endDate   结束
     * @return
     */
    public static int calculateMonthIn(Date startDate, Date endDate) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(startDate);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(endDate);
        int c = (cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR)) * 12
                + cal2.get(Calendar.MONTH) - cal1.get(Calendar.MONTH);
        return c;
    }

    /**
     * 取得传入时间的相差天数
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public static int daysOfTwo(Date startDate, Date endDate) {
        if (null == startDate || null == endDate) {
            return -1;
        }
        long intervalMilli = endDate.getTime() - startDate.getTime();
        return (int) (intervalMilli / (24 * 60 * 60 * 1000));
    }


    public static boolean days(Date start, Date endDate, Date conterDate) {
        boolean falg = true;
        if (conterDate.getTime() > endDate.getTime() || conterDate.getTime() < start.getTime()) {
            falg = false;
        }
        return falg;
    }

    /**
     * 功能: 返回date1与date2相差的分钟数
     *
     * @param date1
     * @param date2
     * @return int
     */
    public static int minDiff(Date date1, Date date2) {
        int i = (int) ((date1.getTime() - date2.getTime()) / 1000 / 60);
        return i;
    }

    /**
     * 获取连个时间相隔秒钟数
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public static long calLastedTime(Date startDate, Date endDate) {
        long time = (endDate.getTime() - startDate.getTime()) / 1000;
        return time;
    }

    /**
     * 取得日期的周一
     *
     * @param date
     * @return
     */
    public static String getWeekFirstDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DATE, -1);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        return dateFormat.format(calendar.getTime());
    }

    public static List<String> dateList(String startDateStr, String endDateStr) {
        List<String> rtn = new ArrayList<String>();
        Date startDate = stringToDate(startDateStr, DATE_SHORT_FORMAT);
        Date endDate = stringToDate(endDateStr, DATE_SHORT_FORMAT);
        while (startDate.compareTo(endDate) < 0) {
            rtn.add(dateToString(startDate, DATE_SHORT_FORMAT));
            startDate = addDays(startDate, 1);
        }
        return rtn;
    }

    /**
     * 取得当前日期所在周
     *
     * @param date
     * @return
     */
    public static int getWeekOfDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int w = cal.get(Calendar.DAY_OF_WEEK);
        if (w < 0) {
            w = 0;
        }
        return w;
    }

    /**
     * 取得当前秒
     *
     * @param date
     * @return
     */
    public static int getSecond(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int w = cal.get(Calendar.SECOND);
        return w;
    }

    /**
     * 取得当前分
     *
     * @param date
     * @return
     */
    public static int getMinute(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int w = cal.get(Calendar.MINUTE);
        return w;
    }

    public static long changeSecond(long second) {
        return second * 1000;
    }

    /**
     * 获取随机日期
     *
     * @param beginDate 起始日期，格式为：yyyy-MM-dd
     * @param endDate   结束日期，格式为：yyyy-MM-dd
     * @return
     */
    public static Date randomDate(String beginDate, String endDate) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Date start = format.parse(beginDate);
            Date end = format.parse(endDate);

            if (start.getTime() >= end.getTime()) {
                return null;
            }

            long date = random(start.getTime(), end.getTime());

            return new Date(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据开始数和结束数字获取随机数据
     *
     * @param begin
     * @param end
     * @return
     */
    public static long random(long begin, long end) {
        long rtn = begin + (long) (Math.random() * (end - begin));
        if (rtn == begin || rtn == end) {
            return random(begin, end);
        }
        return rtn;
    }

    /**
     * <b>方法名：</b> differentDays<br>
     * <b>功能说明：</b> 比较2个时间天数（并非24小时）<br>
     *
     * @param date1
     * @param date2
     * @author <font color='blue'>徐恩源</font>
     * @date 2019-04-08 20:26:38
     */
    public static int differentDays(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        int day1 = cal1.get(Calendar.DAY_OF_YEAR);
        int day2 = cal2.get(Calendar.DAY_OF_YEAR);

        int year1 = cal1.get(Calendar.YEAR);
        int year2 = cal2.get(Calendar.YEAR);
        // 不同年
        if (year1 != year2) {
            int timeDistance = 0;
            for (int i = year1; i < year2; i++) {
                //闰年
                if (i % 4 == 0 && i % 100 != 0 || i % 400 == 0) {
                    timeDistance += 366;
                } else {
                    //不是闰年
                    timeDistance += 365;
                }
            }

            return timeDistance + (day2 - day1);
        } else {//同一年
            return day2 - day1;
        }
    }

    /**
     * @return
     * @方法名：取得指定日期的最小时间（yyyy-MM-dd 0:0:0）
     * @author lqd
     * @date Dec 19, 2013 11:16:22 AM
     */
    public static Date getMintimeOfDay(Date date) {
        Calendar min = Calendar.getInstance();
        min.setTime(date);
        min.set(Calendar.HOUR_OF_DAY, min.getActualMinimum(Calendar.HOUR_OF_DAY));
        min.set(Calendar.MINUTE, min.getActualMinimum(Calendar.MINUTE));
        min.set(Calendar.SECOND, min.getActualMinimum(Calendar.SECOND));
        min.set(Calendar.MILLISECOND, min.getActualMinimum(Calendar.MILLISECOND));
        return min.getTime();
    }

    /**
     * @return
     * @方法名：取得指定日期的最大时间（yyyy-MM-dd 23:59:59）
     * @author lqd
     * @date Dec 19, 2013 11:16:22 AM
     */
    public static Date getMaxtimeOfDay(Date date) {
        Calendar max = Calendar.getInstance();
        max.setTime(date);
        max.set(Calendar.HOUR_OF_DAY, max.getActualMaximum(Calendar.HOUR_OF_DAY));
        max.set(Calendar.MINUTE, max.getActualMaximum(Calendar.MINUTE));
        max.set(Calendar.SECOND, max.getActualMaximum(Calendar.SECOND));
        max.set(Calendar.MILLISECOND, max.getActualMaximum(Calendar.MILLISECOND));
        return max.getTime();
    }

    /**
     * 取得传入时间的开始时间
     *
     * @param date
     * @return
     */
    public static Date getStartDate(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(DATE_SHORT_FORMAT);
        SimpleDateFormat format1 = new SimpleDateFormat(DATE_WITHSECOND_FORMAT);
        String temp = format.format(date);
        temp += " 00:00:00";
        try {
            return format1.parse(temp);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 得到指定日期的一天的开始时刻00:00:00
     *
     * @param date
     * @return
     */
    public static Date getFinallyDate(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(DATE_SHORT_FORMAT);
        SimpleDateFormat format1 = new SimpleDateFormat(DATE_WITHSECOND_FORMAT);
        String temp = format.format(date);
        temp += " 23:59:59";
        try {
            return format1.parse(temp);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取昨天的开始时间 格式：yyyy-MM-dd HH:mm:ss
     *
     * @return
     */
    public static Date getYesterdayStartTime() {
        Date dNow = new Date(); //当前时间
        Date dBefore = new Date();
        Calendar calendar = Calendar.getInstance(); //得到日历bai
        calendar.setTime(dNow);//把当前时间赋给日历
        calendar.add(Calendar.DAY_OF_MONTH, -1); //设置为前一天
        dBefore = calendar.getTime(); //得到前一天的时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); //设置时间格式
        String defaultStartDate = sdf.format(dBefore); //格式化前一天
        defaultStartDate = defaultStartDate + " 00:00:00";
        return stringToDate(defaultStartDate, DATE_WITHSECOND_FORMAT);
    }

    /**
     * 获取昨天的结束时间 格式：yyyy-MM-dd HH:mm:ss
     *
     * @return
     */
    public static Date getYesterdayEndTime() {
        Date dNow = new Date(); //当前时间
        Date dBefore = new Date();
        Calendar calendar = Calendar.getInstance(); //得到日历bai
        calendar.setTime(dNow);//把当前时间赋给日历
        calendar.add(Calendar.DAY_OF_MONTH, -1); //设置为前一天
        dBefore = calendar.getTime(); //得到前一天的时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); //设置时间格式
        String defaultStartDate = sdf.format(dBefore); //格式化前一天
        defaultStartDate = defaultStartDate + " 00:00:00";
        String defaultEndDate = defaultStartDate.substring(0, 10) + " 23:59:59";
        return stringToDate(defaultEndDate, DATE_WITHSECOND_FORMAT);
    }

    /**
     * 获取昨天的开始时间(解决服务器时区不同的问题) 格式：yyyy-MM-dd HH:mm:ss
     *
     * @return
     */
    public static Date yesterdayStartTime() {
        Date dNow = new Date(); //当前时间
        //时区晚8小时
        long time=(long)(dNow.getTime()+8 * 60 * 60 * 1000);
        Date dBefore = new Date();
        Calendar calendar = Calendar.getInstance(); //得到日历bai
        calendar.setTimeInMillis(time);//把当前时间赋给日历
        calendar.add(Calendar.DAY_OF_MONTH, -1); //设置为前一天
        dBefore = calendar.getTime(); //得到前一天的时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); //设置时间格式
        String defaultStartDate = sdf.format(dBefore); //格式化前一天
        defaultStartDate = defaultStartDate + " 00:00:00";
        return stringToDate(defaultStartDate, DATE_WITHSECOND_FORMAT);
    }

    /**
     * 获取昨天的结束时间 格式：yyyy-MM-dd HH:mm:ss
     *
     * @return
     */
    public static Date yesterdayEndTime() {
        Date dNow = new Date(); //当前时间
        //时区晚8小时
        long time=(long)(dNow.getTime()+8 * 60 * 60 * 1000);
        Date dBefore = new Date();
        Calendar calendar = Calendar.getInstance(); //得到日历bai
        calendar.setTimeInMillis(time);//把当前时间赋给日历
        calendar.add(Calendar.DAY_OF_MONTH, -1); //设置为前一天
        dBefore = calendar.getTime(); //得到前一天的时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); //设置时间格式
        String defaultStartDate = sdf.format(dBefore); //格式化前一天
        defaultStartDate = defaultStartDate + " 00:00:00";
        String defaultEndDate = defaultStartDate.substring(0, 10) + " 23:59:59";
        return stringToDate(defaultEndDate, DATE_WITHSECOND_FORMAT);
    }

    /**
     * 获取传入时间的前一天结束时间，格式:yyyy-MM-dd HH:mm:ss
     *
     * @return
     */
    public static String getTheDayBeforeEndTime(String date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date dBefore;
            //得到日历bai
            Calendar calendar = Calendar.getInstance();
            //把当前时间赋给日历
            calendar.setTime(dateFormat.parse(date));
            //设置为前一天
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            //得到前一天的时间
            dBefore = calendar.getTime();
            //设置时间格式
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            //格式化前一天
            String defaultStartDate = sdf.format(dBefore);
            defaultStartDate = defaultStartDate + " 00:00:00";
            return defaultStartDate.substring(0, 10) + " 23:59:59";
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 将时间格式的字符串转为时间戳
     */
    public static Long getTimeStamp(String time) throws ParseException {
    SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date date = format.parse(time);
    return date.getTime();
    }

    /**
     * 计算某日期所在季度开始日期
     * 季度划分：1、2、3， 4、5、6， 7、8、9， 10、11、12
     */
    public static Date getSeasonEndDate (Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH);
        calendar.set(Calendar.MONTH, (month + 3) / 3 * 3);
        calendar.set(Calendar.DATE, 1);
        return new Date(calendar.getTime().getTime() - 24 * 60 * 60 *1000);
    }
    /**
     * 计算某日期所在季度结束日期
     * 季度划分：1、2、3， 4、5、6， 7、8、9， 10、11、12
     */
    public static Date getSeasonStartDate (Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH);
        calendar.set(Calendar.MONTH, month / 3 * 3);
        calendar.set(Calendar.DATE, 1);
        return calendar.getTime();
    }

    /**
     * 获取当前日期上一季度 开始时间
     *
     * @return
     */
    public static Date getStartQuarter(Date date) {
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(date);
        startCalendar.set(Calendar.MONTH, ((int) startCalendar.get(Calendar.MONTH) / 3 - 1) * 3);
        startCalendar.set(Calendar.DAY_OF_MONTH, 1);
        setMinTime(startCalendar);
        return startCalendar.getTime();
    }
    /**
     * 获取当前日期上一季度 结束时间
     *当前日期为31号存在bug已修复
     * @return
     */
    public static Date getLastQuarter(Date date) {


        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(date);
        int month = endCalendar.get(Calendar.MONTH);
        endCalendar.set(Calendar.MONTH, (month/ 3 - 1) * 3 + 3);
        endCalendar.set(Calendar.DATE, 1);
        return new Date(endCalendar.getTime().getTime() - 24 * 60 * 60 *1000);

    }

    /**
     * 最小时间
     *
     * @param calendar
     */
    private static void setMinTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    /**
     * 最大时间
     *
     * @param calendar
     */
    private static void setMaxTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getActualMaximum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getActualMaximum(Calendar.MILLISECOND));
    }


    /**
     * 获取时间段内所有季度
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     */
    public static List<String> getSeasonList(LocalDate startTime, LocalDate endTime) {
        // 取当月第一天, 避免startTime的日期大于endTime计算不出来的情况
        startTime = LocalDate.of(startTime.getYear(), startTime.getMonthValue(), 1);
        endTime = LocalDate.of(endTime.getYear(), endTime.getMonthValue(), 1);
        Set<String> set = new HashSet<>();
        LocalDate mark = startTime;
        while (true) {
            if (mark.isBefore(endTime) || mark.isEqual(endTime)) {
                String season = String.valueOf(mark.getYear())+"年" + String.valueOf((mark.getMonthValue() + 2) / 3+"季度");
                set.add(season);
                // 加一个月
                mark = mark.plusMonths(1);
            } else {
                break;
            }
        }
        System.out.println(set);
        // set中是倒序, 重新排序
        return set.stream().sorted().collect(Collectors.toList());
    }


    /**
     * 获取指定月份的开始时间
     * @param date
     * @return
     */
    public static Date getFirstDateOfMonth(Date date){
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        instance.set(Calendar.DAY_OF_MONTH,1);
        return instance.getTime();
    }

    /**
     * 获取指定月份的结束时间
     * @param date
     * @return
     */
    public static Date getLastDateOfMonth(Date date){
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        instance.set(Calendar.DAY_OF_MONTH,instance.getActualMaximum(Calendar.DAY_OF_MONTH));
        return instance.getTime();
    }



}
