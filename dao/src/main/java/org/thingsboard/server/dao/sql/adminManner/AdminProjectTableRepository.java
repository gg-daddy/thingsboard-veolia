package org.thingsboard.server.dao.sql.adminManner;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.thingsboard.server.dao.model.sql.AdminProjectTableEntity;
import org.thingsboard.server.dao.util.SqlDao;

@SqlDao
public interface AdminProjectTableRepository extends JpaRepository<AdminProjectTableEntity, String>, JpaSpecificationExecutor<AdminProjectTableEntity> {



}
