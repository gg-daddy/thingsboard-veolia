package org.thingsboard.server.dao.sql.adminManner;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.thingsboard.server.dao.model.sql.CertenTableEntity;
import org.thingsboard.server.dao.util.SqlDao;

@SqlDao
public interface CertenTableRepository extends JpaRepository<CertenTableEntity, String>, JpaSpecificationExecutor<CertenTableEntity> {


}
