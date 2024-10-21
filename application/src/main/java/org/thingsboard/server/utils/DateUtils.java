package org.thingsboard.server.utils;/*
 * @Author:${zhangrui}
 * @Date:2020/9/24 9:18
 */

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DateUtils {


    /***
     * 获取到当前时间  --- 到时分秒
     *
     * @return
     */
    public static String getNow() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }


    /***
     * 获取上个月的月份
     * @return
     */
    public static String getLastMonth() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月");


        Calendar calendar = Calendar.getInstance();

        calendar.add(Calendar.MONTH, -1);//上个月

        return format.format(calendar.getTime());

    }


    public static Long getLastMonthStartTime() throws Exception {
        Long currentTime = System.currentTimeMillis();

        String timeZone = "GMT+8:00";
        Calendar calendar = Calendar.getInstance();// 获取当前日期
        calendar.setTimeZone(TimeZone.getTimeZone(timeZone));
        calendar.setTimeInMillis(currentTime);
        calendar.add(Calendar.YEAR, 0);
        calendar.add(Calendar.MONTH, -1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);// 设置为1号,当前日期既为本月第一天
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }


    public static Long getLastMonthEndTime() {
        Long currentTime = System.currentTimeMillis();

        String timeZone = "GMT+8:00";
        Calendar calendar = Calendar.getInstance();// 获取当前日期
        calendar.setTimeZone(TimeZone.getTimeZone(timeZone));
        calendar.setTimeInMillis(currentTime);
        calendar.add(Calendar.YEAR, 0);
        calendar.add(Calendar.MONTH, -1);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));// 获取当前月最后一天
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);

        return calendar.getTimeInMillis();
    }

    /**
     * 获取到一天的时间
     *
     * @return
     */
    public static long getDayTime() {

        Long time = 24 * 60 * 60 * 1000L;
        return time;
    }


    public static long getMonth() {

        Long time = 24 * 60 * 60 * 1000L*31;
        return time;
    }



    /**
     * 字符串变成时间
     *
     * @param timestamp
     * @return
     */
    public static String getDate(Long timestamp) {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(timestamp);
        return date;
    }


    public static String getDateForHour(Long timestamp) {
        String date = new SimpleDateFormat("yyyy-MM-dd HH").format(timestamp);
        return date;
    }


    public static String getDateAll(Long timestamp) {
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);
        return date;
    }

    public static String getDateAllNoMiao(Long timestamp) {
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(timestamp);
        return date;
    }


    /***
     * str型转换成 Long型
     * 有开始时间和结束时间
     * @param
     * @return
     */
    public static Map<String,Long> strToDateLong(String strDate) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date date = format.parse(strDate);
        Map<String,Long> map = new HashMap<>();
        map.put("startTime",date.getTime());
        map.put("endTime",date.getTime()+DateUtils.getDayTime());
        return map;
    }


    public static Map<String,Long> strToDateLongForMonth(String strDate) throws ParseException {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
        Date date = format.parse(strDate);
        Map<String,Long> map = new HashMap<>();
        map.put("startTimeForMonth",date.getTime());

        Calendar ca = format.getCalendar();
        ca.set(Calendar.DAY_OF_MONTH, ca.getActualMaximum(Calendar.DAY_OF_MONTH));
        map.put("endTimeForMonth",ca.getTime().getTime());
        return map;
    }


    /***
     * 获取上个月的的开始时间
     * @return
     */
    public static Map getLastMonthTime() throws Exception {
        Long startTime = getLastMonthStartTime();
        Long endTime = getLastMonthEndTime();
        DateTimeFormatter ftf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTimeStr = ftf.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault()));
        String endTimeStr = ftf.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.systemDefault()));
        Map map = new HashMap();
        map.put("startDate", startTimeStr);
        map.put("endDate", endTimeStr);
        return map;
    }


    private static SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月");

    /***
     * 获取该月有多少天
     * @return
     */
    public static int getAllDay(String source) {

        try {
            Date date = format.parse(source);
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(date);
            return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }


    /***
     * 获取到导出的日期开始的时间
     * @param dateStr
     * @return
     */
    public static Long getDayStartWorkTime(String dateStr) {
        DateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = sd.parse(dateStr);
            return date.getTime() + 9 * 60 * 60 * 1000;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0L;
    }


    /***
     * 获取到导出的日期结束的时间
     * @param dateStr
     * @return
     */
    public static Long getDayEndWorkTime(String dateStr) {
        DateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = sd.parse(dateStr);
            return date.getTime() + 17 * 60 * 60 * 1000;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0L;
    }


    /***
     * 获取每小时的时间
     * @return
     */
    public static Long getHourTime() {
        return 60 * 60 * 1000L;
    }



    // 获取今天零点的时间戳
    public static Long getStartTime() {
        Calendar todayStart = Calendar.getInstance();
        todayStart.setTime(new Date());
        todayStart.set(Calendar.HOUR, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);
        return todayStart.getTime().getTime() - DateUtils.getHourTime()*12 ;
    }





    // 获取今天24点的时间戳
    public static Long getEndTime() {
        Calendar todayEnd = Calendar.getInstance();
        todayEnd.setTime(new Date());
        todayEnd.set(Calendar.HOUR, 23);
        todayEnd.set(Calendar.MINUTE, 59);
        todayEnd.set(Calendar.SECOND, 59);
        todayEnd.set(Calendar.MILLISECOND, 999);
        return todayEnd.getTime().getTime()- DateUtils.getHourTime()*12 ;
    }


    // 获取今天24点的时间戳
    public static Long getLastEndTime() {
        Calendar todayEnd = Calendar.getInstance();
        todayEnd.set(Calendar.HOUR, 23);
        todayEnd.set(Calendar.MINUTE, 59);
        todayEnd.set(Calendar.SECOND, 59);
        todayEnd.set(Calendar.MILLISECOND, 999);
        return todayEnd.getTime().getTime();
    }





    public static void main(String[] args) throws Exception {
//        System.out.println(strToDateLong("2021-06-08"));
        Map<String, Long> month = strToDateLongForMonth("2021-07");
        System.out.println(month);
    }


    /**
     * 昨天开始的时间
     * @return
     */
    public static Long getLastStartTime() {
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);
        return todayStart.getTime().getTime() ;
    }

    public static Long getBeginDayOfYesterday() {
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        return cal.getTimeInMillis();
    }




    public static Long getEndDayOfYesterDay() {

        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.add(Calendar.DAY_OF_MONTH, -1);


        return cal.getTimeInMillis();
    }




}
