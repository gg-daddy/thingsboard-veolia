package org.thingsboard.server.config;/*
 * @Author:${zhangrui}
 * @Date:2020/9/29 10:42
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.CassandraClusterFactoryBean;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.stereotype.Service;


@Configuration
@EnableCassandraRepositories
@PropertySource(value = { "classpath:thingsboard.yml" })
@Service
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Autowired
    private Environment env;

    @Value("${cassandra.keyspace_name}")
    protected String keySpace;


    @Override
    protected String getKeyspaceName() {
        return keySpace;
    }



    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.CREATE_IF_NOT_EXISTS;
    }

    public CassandraClusterFactoryBean cluster() {

        CassandraClusterFactoryBean cluster = new CassandraClusterFactoryBean();
        cluster.setContactPoints(env.getProperty("cassandra.contactpoints"));
        cluster.setUsername(env.getProperty("cassandra.username"));
        cluster.setPassword(env.getProperty("cassandra.password"));
        cluster.setPort(new Integer(env.getProperty("cassandra.port")));
        return cluster;
    }
}


