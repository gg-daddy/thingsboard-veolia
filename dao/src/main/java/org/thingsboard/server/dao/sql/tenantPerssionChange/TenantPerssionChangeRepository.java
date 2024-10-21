package org.thingsboard.server.dao.sql.tenantPerssionChange;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.thingsboard.server.dao.model.sql.TenantPerssionChangeTableEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

@SqlDao
public interface TenantPerssionChangeRepository extends JpaRepository<TenantPerssionChangeTableEntity, String>, JpaSpecificationExecutor<TenantPerssionChangeTableEntity> {

    List<TenantPerssionChangeTableEntity> findByTenantId(String tenantId);


    /**
     * 通过用户名查询
     *
     * @param username
     * @return
     */
    List<TenantPerssionChangeTableEntity> findByUsername(String username);



    TenantPerssionChangeTableEntity findByProjectName(String name);





}
