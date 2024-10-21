package org.thingsboard.server.dao.model.sql;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@Table(name = "admin_table")
public class AdminTableEntity implements Serializable {


    private static final long serialVersionUID = -3828187334159624576L;
    /**
     * web 管理员的主键
     */
    @Id
    @Column(name = "id")
    private String id;
    /**
     * 用户名
     */
    @Column(name = "username")
    private String username;
    /**
     * 密码
     */
    @Column(name = "password")
    private String password;
    /**
     * 日期
     */
    @Column(name = "login_time")
    private String loginTime;
    /**
     * 调整的url
     */
    @Column(name = "aim_url")
    private String aimUrl;

    /**
     * 管理员的token
     */
    @Column(name = "admin_token")
    private String adminToken;

    /***
     * 错误的url
     */
    @Column(name = "err_url")
    private String errUrl;
}
