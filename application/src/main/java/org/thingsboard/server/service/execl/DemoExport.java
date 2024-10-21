package org.thingsboard.server.service.execl;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
public class DemoExport implements Serializable {
    private static final long serialVersionUID = 8255798865978449860L;

    private UUID uuid;

    /**
     * 点位
     */
    private String pointPosition;
    /**
     * 日期
     */
    private String date;


    private Long dateTime;

    /**
     *
     * 温度
     */
    private String temperature;
    /**
     * 湿度
     */
    private String humidity;
    /**
     * pm2.5
     */
    private String  pM25;
    /**
     * pm10
     */
    private String pm10;
    /**
     * pm1.0
     */
    private String pm100;
    /**
     * co2
     */
    private String co2;
    /**
     * 甲醛
     */
    private String cH2o;




}