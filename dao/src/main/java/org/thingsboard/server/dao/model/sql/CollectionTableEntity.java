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
@Table(name = "collection_table")
public class CollectionTableEntity implements Serializable {

    private static final long serialVersionUID = 2322634382700549028L;
    /**
     * 工单id
     */
    @Id
    @Column(name = "collection_id")
    private String collectionId;

    /**
     * 设备的id
     */
    @Column(name = "device_id")
    private String deviceId;


    /**
     * 用户的id
     */
    @Column(name = "user_id")
    private String userId;

    /**
     * 电话号码
     */
    @Column(name = "phone")
    private String phone;



}
