package org.thingsboard.server.dao.sql.adminManner;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.thingsboard.server.dao.model.sql.AdminTableEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Optional;

@SqlDao
public interface AdminTableRepository extends JpaRepository<AdminTableEntity, String>, JpaSpecificationExecutor<AdminTableEntity> {


    Optional<AdminTableEntity> findByUsername(String username);

}
