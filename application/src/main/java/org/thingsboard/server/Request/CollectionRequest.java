package org.thingsboard.server.Request;

import lombok.Data;

import java.io.Serializable;

@Data
public class CollectionRequest implements Serializable {


    private static final long serialVersionUID = 80800930558754563L;

    private String deviceIds;


    private Boolean flag;

    private String type;


    private String phone;


}
