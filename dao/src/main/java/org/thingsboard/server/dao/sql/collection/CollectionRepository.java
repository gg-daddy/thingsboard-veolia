package org.thingsboard.server.dao.sql.collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.thingsboard.server.dao.model.sql.CollectionTableEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

@SqlDao
public interface CollectionRepository extends JpaRepository<CollectionTableEntity, String>, JpaSpecificationExecutor<CollectionTableEntity> {

    /***
     * 删除
     * @param deviceId
     * @param userId
     */
    void deleteByDeviceIdAndUserIdAndPhone(String deviceId, String userId, String phone);


    void deleteByDeviceIdAndUserId(String deviceId, String userId);


    /***
     * 查询id和电话号码
     * @param userId
     * @return
     */
    List<CollectionTableEntity> findByUserIdAndPhone(String userId, String phone);



    /***
     * 查询id和电话号码
     * @param userId
     * @return
     */
    List<CollectionTableEntity> findByUserId(String userId);

}
