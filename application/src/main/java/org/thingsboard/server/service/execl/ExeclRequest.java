package org.thingsboard.server.service.execl;/*
 * @Author:${zhangrui}
 * @Date:2020/9/27 15:44
 */

import lombok.Data;

import java.io.Serializable;

@Data
public class ExeclRequest implements Serializable{
    private static final long serialVersionUID = 9194980472958979514L;


    private String labal;

    private String value;
}
