package org.thingsboard.server.Response;

import lombok.Data;

import java.io.Serializable;

@Data
public class NewResponse implements Serializable {
    private static final long serialVersionUID = 6204915648406072504L;

    private Long pm25;


    private Long aQI;


}
