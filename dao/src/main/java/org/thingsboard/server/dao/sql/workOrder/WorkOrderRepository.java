package org.thingsboard.server.dao.sql.workOrder;/*
 * @Author:${zhangrui}
 * @Date:2020/9/16 17:45
 */

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.FeedbackEntity;
import org.thingsboard.server.dao.model.sql.UserEntity;
import org.thingsboard.server.dao.model.sql.WorkOrderEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

@SqlDao
public interface WorkOrderRepository extends JpaRepository<WorkOrderEntity, String>,JpaSpecificationExecutor<WorkOrderEntity> {


    /***
     * 查询需要自动更新的数据
     * @param equipment
     * @param type
     * @param tenantId
     * @return
     */
       List<WorkOrderEntity> findByEquipmentNumberAndTypeAndTenantIdOrderByDateDesc(String equipment,String type,String tenantId);


}
