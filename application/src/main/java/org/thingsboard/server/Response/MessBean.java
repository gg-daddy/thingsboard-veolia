package org.thingsboard.server.Response;

import lombok.Data;

import java.io.Serializable;

@Data
public class MessBean implements Serializable {
    private static final long serialVersionUID = -5610183222081145188L;


    private String tenantId;



    private String tenantName;

}
