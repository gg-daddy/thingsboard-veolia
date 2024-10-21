package org.thingsboard.server.dao.sql.projectList;/*
 * @Author:${zhangrui}
 * @Date:2020/9/16 17:45
 */

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.thingsboard.server.dao.model.sql.FeedbackEntity;
import org.thingsboard.server.dao.model.sql.ProjectEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

@SqlDao
public interface ProjectRepository extends JpaRepository<ProjectEntity, String>,JpaSpecificationExecutor<ProjectEntity>
{
    /***
     * 查询
     * @param list
     * @return
     */
    List<ProjectEntity> findByIdIn(List<String> list);
}
