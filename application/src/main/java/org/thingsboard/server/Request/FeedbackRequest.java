package org.thingsboard.server.Request;/*
 * @Author:${zhangrui}
 * @Date:2020/9/23 17:41
 */

import lombok.Data;

import java.io.Serializable;

@Data
public class FeedbackRequest implements Serializable {

    private static final long serialVersionUID = 4604719356911227529L;


    /**
     * 姓名
     */
    private String name;

    private String contact;

    private String content;

    private String projectId;

}
