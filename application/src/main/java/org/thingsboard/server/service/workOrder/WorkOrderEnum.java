package org.thingsboard.server.service.workOrder;/*
 * @Author:${zhangrui}
 * @Date:2020/9/22 17:39
 */

public enum WorkOrderEnum {

    TEMPERATURE("温度", "0~40"),

    HUMIDITY("湿度", "0~90"),

    PM25("PM2.5", "0~75"),

    PM10("PM10", "0~75"),

    CH20("甲醛", "0~80"),

    //PM1.0的写法
    PM100("PM1.0", "0~80"),

    CO2("CO₂", "0~1000");

    private String code;

    private String name;




    WorkOrderEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String getCode() {
        return this.code;
    }


}
