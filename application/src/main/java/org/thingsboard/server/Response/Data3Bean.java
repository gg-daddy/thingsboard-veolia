package org.thingsboard.server.Response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Data3Bean implements Serializable {
    private static final long serialVersionUID = 8829501922579431181L;

    private String id;


    private List<Object> pm25;


    private List<Object> pm10;

    private List<Object> CH2O;

    private List<Object> CO2;

    private List<Object> humidity;

    private List<Object> temperature;


}
