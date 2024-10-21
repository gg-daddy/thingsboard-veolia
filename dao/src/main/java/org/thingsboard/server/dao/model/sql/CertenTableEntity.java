package org.thingsboard.server.dao.model.sql;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@Table(name = "certen_table")
public class CertenTableEntity implements Serializable {

    private static final long serialVersionUID = 3873989237085991803L;
    @Id
    @Column(name = "center_id")
    private String centerId;

    @Column(name = "longitude")
    private String longitude;

    @Column(name = "latitude")
    private String latitude;

    @Column(name = "level")
    private String level;



}
