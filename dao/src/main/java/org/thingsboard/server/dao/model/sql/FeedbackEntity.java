package org.thingsboard.server.dao.model.sql;

/*
 * @Author:${zhangrui}
 * @Date:2020/9/23 17:24
 */

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@Table(name = "feedback")
public class FeedbackEntity implements Serializable {

    private static final long serialVersionUID = -2766737348359101715L;

    @Id
    @Column(name = "id")
    private String id;

    /**
     * 反馈方式
     */
    @Column(name = "contact")
    private String contact;

    /**
     * 反馈内容
     */
    @Column(name = "content")
    private String content;

    /***
     * 提交者姓名
     */
    @Column(name = "name")
    private String name;

    /***
     * 项目的id
     */
    @Column(name = "project_id")
    private String projectId;


    /**
     * 提交时间
     */
    @Column(name = "date")
    private String date;


}
