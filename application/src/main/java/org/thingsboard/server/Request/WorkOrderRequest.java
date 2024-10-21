package org.thingsboard.server.Request;/*
 * @Author:${zhangrui}
 * @Date:2020/9/16 18:09
 */

import lombok.Data;

import java.io.Serializable;

@Data
public class WorkOrderRequest implements Serializable {

    private static final long serialVersionUID = 2135648578837060874L;

    /**
     * 设备号
     */
    private String equipmentNumber;

    /**
     * 申请的内容
     */
    private String applicationContent;

    /**
       操作者
     */
    private String operator;

    /**
     * 电话号码
     */
    private String phone;

}
