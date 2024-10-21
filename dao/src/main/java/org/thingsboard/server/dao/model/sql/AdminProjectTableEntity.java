package org.thingsboard.server.dao.model.sql;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@Table(name = "admin_project_table")
public class AdminProjectTableEntity implements Serializable {

    private static final long serialVersionUID = -1537924597846400728L;

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "project_lgt")
    private String projectLgt;

    @Column(name = "project_lat")
    private String projectLat;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "project_desc")
    private String projectDesc;

    @Column(name = "project_address")
    private String projectAddress;

    @Column(name = "project_username")
    private String projectUsername;

    @Column(name = "project_password")
    private String projectPassword;


    @Column(name = "aim_url")
    private String aimUrl;


}
