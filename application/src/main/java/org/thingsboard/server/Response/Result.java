package org.thingsboard.server.Response;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result implements Serializable {
    private static final long serialVersionUID = -8232988896443160083L;

    public Integer code;

    private UserBean userBean;

    public Result(Integer code, UserBean userBean) {
        this.code = code;
        this.userBean = userBean;
    }

}
