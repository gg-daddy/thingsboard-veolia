package org.thingsboard.server.Request;

import lombok.Data;

import java.io.Serializable;

@Data
public class ExportRequest implements Serializable {

    private static final long serialVersionUID = 80800930558754563L;


    private String equipmentNumber;


    private String startTime;


    private String endTime;

}
