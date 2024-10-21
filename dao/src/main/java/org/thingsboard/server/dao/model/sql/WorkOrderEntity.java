package org.thingsboard.server.dao.model.sql;/*
 * @Author:${zhangrui}
 * @Date:2020/9/16 15:34
 */

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@Table(name = "work_order")
public class WorkOrderEntity implements Serializable {

    private static final long serialVersionUID = 2322634382700549028L;
    /**
     * 工单id
     */
    @Id
    @Column(name = "id")
    private String id;

    /**
     * 申诉的内容
     */
    @Column(name = "application_content")
    private String applicationContent;


    /**
     * 处理的内容
     */
    @Column(name = "deal_content")
    private String dealContent;

    /**
     * 日期
     */
    @Column(name = "date")
    private String date;

    /**
     * 状态
     */
    @Column(name = "status")
    private Integer status;

    /**
     * 处理者
     */
    @Column(name = "operator")
    private String operator;

    /**
     * 设备编号
     */
    @Column(name = "equipment_number")
    private String equipmentNumber;

    /**
     * 租户的id
     */
    @Column(name = "tenant_id")
    private String tenantId;

    /**
     * 电话号码
     */
    @Column(name = "phone")
    private String phone;

    /**
      问题的类型
     */
    @Column(name = "type")
    private String type;

    /**
     * 处理时间
     */
    @Column(name = "deal_date")
    private String dealDate;

}
