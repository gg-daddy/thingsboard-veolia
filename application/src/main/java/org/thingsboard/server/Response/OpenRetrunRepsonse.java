package org.thingsboard.server.Response;

import lombok.Data;

import java.io.Serializable;

@Data
public class OpenRetrunRepsonse implements Serializable {
    private static final long serialVersionUID = -4068810866450698915L;

    private String session_key;

    private String openid;


}
