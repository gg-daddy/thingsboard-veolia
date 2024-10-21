package org.thingsboard.server.Request;

import lombok.Data;

import java.io.Serializable;

@Data
public class WebUserRequest implements Serializable {
    private static final long serialVersionUID = -8992622800425622432L;

    private String username;


    private String password;
}
