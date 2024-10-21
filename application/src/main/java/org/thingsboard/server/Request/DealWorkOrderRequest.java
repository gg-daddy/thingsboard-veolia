package org.thingsboard.server.Request;/*
 * @Author:${zhangrui}
 * @Date:2020/9/23 13:12
 */

import lombok.Data;

import java.io.Serializable;

@Data
public class DealWorkOrderRequest implements Serializable {

    private static final long serialVersionUID = -4837484295944327566L;

    /***
     * 工单的工号
     */
    private String id;

    /**
     * 工单的处理信息
     */
    private String dealContent;

    /**
     * 电话号码
     */
    private String phone;


    /**
     * 处理人
     */
    private String operator;



}
