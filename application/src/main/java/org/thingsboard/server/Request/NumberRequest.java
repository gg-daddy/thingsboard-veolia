package org.thingsboard.server.Request;

import lombok.Data;

import java.io.Serializable;

@Data
public class NumberRequest implements Serializable {
    private static final long serialVersionUID = 7998743018951493774L;


    private String encryptedData;


    private String sessionKey;


    private String iv;


    private String phone;
}
