package org.thingsboard.server.dao.model.sql;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@Table(name = "tenant_perssion_change_table")
public class TenantPerssionChangeTableEntity implements Serializable {
    private static final long serialVersionUID = -8038123092668125740L;

    @Id
    @Column(name = "id")
    private String id;


    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "weather_id")
    private String weatherId;

    @Column(name = "sum_id")
    private String sumId;




}
