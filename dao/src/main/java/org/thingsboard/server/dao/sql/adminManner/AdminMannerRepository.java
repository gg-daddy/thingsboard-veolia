package org.thingsboard.server.dao.sql.adminManner;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.thingsboard.server.dao.model.sql.AdminMannerEntity;
import org.thingsboard.server.dao.util.SqlDao;

@SqlDao
public interface AdminMannerRepository extends JpaRepository<AdminMannerEntity, String>, JpaSpecificationExecutor<AdminMannerEntity> {


    AdminMannerEntity findByPhone(String phone);

}
