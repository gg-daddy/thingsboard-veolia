package org.thingsboard.server.dao.sql.Feedback;/*
 * @Author:${zhangrui}
 * @Date:2020/9/16 17:45
 */

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.thingsboard.server.dao.model.sql.FeedbackEntity;
import org.thingsboard.server.dao.model.sql.WorkOrderEntity;
import org.thingsboard.server.dao.util.SqlDao;

@SqlDao
public interface FeedbackRepository extends JpaRepository<FeedbackEntity, String>,JpaSpecificationExecutor<FeedbackEntity>
{




}
