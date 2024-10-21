package org.thingsboard.server.Response;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserBean implements Serializable {
    private static final long serialVersionUID = 2469515680862226250L;

    private String username;

    private String tenantId;

    private String token;

    private String refreshToken;

    private String renantName;

    private String sumId;
}
