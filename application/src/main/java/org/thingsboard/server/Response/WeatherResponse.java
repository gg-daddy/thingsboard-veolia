package org.thingsboard.server.Response;

import lombok.Data;

import java.io.Serializable;

@Data
public class WeatherResponse implements Serializable {
    private static final long serialVersionUID = -7395506351741023383L;


    private String maxTem;

    private String minTem;

    /***
     * 天气等级
     */
    private Integer weatherLevel;


    private String weatherDesc;

    /**
     * 空气质量
     */
    private Integer airLevel;


    private String airLevelDesc;




}
