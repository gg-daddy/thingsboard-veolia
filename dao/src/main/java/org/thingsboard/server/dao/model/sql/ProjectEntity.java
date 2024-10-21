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
@Table(name = "project")
public class ProjectEntity implements Serializable {


    private static final long serialVersionUID = 6562412824465544452L;
    @Id
    @Column(name = "id")
    private String id;


    /***
     * 项目名
     */
    @Column(name = "name")
    private String name;



}
