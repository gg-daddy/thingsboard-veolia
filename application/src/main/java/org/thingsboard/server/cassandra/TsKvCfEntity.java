package org.thingsboard.server.cassandra;/*
 * @Author:${zhangrui}
 * @Date:2020/9/28 13:33
 */


import com.datastax.driver.mapping.annotations.PartitionKey;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import javax.persistence.Entity;
import java.io.Serializable;
import java.util.UUID;


@Data
@Entity
@Table(value = "ts_kv_cf")
public class TsKvCfEntity implements Serializable {
    private static final long serialVersionUID = -4796057462280169777L;

    @PartitionKey(value = 0)
    @PrimaryKey("entity_type")
    private String entityType;

    @PartitionKey(value = 1)
    @Column( value= "entity_id")
    private UUID entityId;

    @PartitionKey(value = 2)
    @Column(value = "key")
    private String key;


    @Column(value = "partition")
    private Long partition;


    @Column(value = "ts")
    public Long ts;

    @Column(value = "bool_v")
    private Boolean boolV;

    @Column(value = "dbl_v")
    private Long dblV;

    @Column(value = "long_v")
    private Long longV;

    @Column(value = "str_v")
    private Long strV;







}
