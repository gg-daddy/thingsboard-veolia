package org.thingsboard.server.dao.sql.tenantPerssion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.thingsboard.server.dao.model.sql.TenantPerssionTableEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Optional;

@SqlDao
public interface TenantPerssionTableRepository extends JpaRepository<TenantPerssionTableEntity, String>, JpaSpecificationExecutor<TenantPerssionTableEntity> {

    /***
     * 根据电话号码查询
     * @param phone
     * @return
     */
    Optional<TenantPerssionTableEntity> findByPhone(String phone);


}
