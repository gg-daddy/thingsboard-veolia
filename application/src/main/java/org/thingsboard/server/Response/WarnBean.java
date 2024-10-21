package org.thingsboard.server.Response;

import lombok.Data;

import java.io.Serializable;

@Data
public class WarnBean implements Serializable {
    private static final long serialVersionUID = 285910376583823766L;

    private String pm25;
}
