package org.thingsboard.server.dao.model.sql;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@Table(name = "admin_manner")
public class AdminMannerEntity implements Serializable {
    private static final long serialVersionUID = -1537924597846400728L;

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "phone")
    private String phone;

    @Column(name = "tenant_id")
    private String tenantId;

}
