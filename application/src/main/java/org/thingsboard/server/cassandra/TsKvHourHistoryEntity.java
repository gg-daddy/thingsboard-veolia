package org.thingsboard.server.cassandra;

import com.datastax.driver.mapping.annotations.PartitionKey;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import javax.persistence.Entity;
import java.io.Serializable;

@Data
@Entity
@Table(value = "ts_kv_hour_history")
public class TsKvHourHistoryEntity implements Serializable {

    private static final long serialVersionUID = -4796057462280111777L;

    @PartitionKey(value = 0)
    @PrimaryKey("entity_id")
    @Column( value= "entity_id")
    private String entityId;

    @PartitionKey(value = 1)
    @Column(value = "key")
    public String key;

    @Column(value = "value")
    public double value;

    @Column(value = "ts")
    public Long ts;


    public String rad;
}
