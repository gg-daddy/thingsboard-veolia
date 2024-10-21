package org.thingsboard.server.Response;

import lombok.Data;
import org.thingsboard.server.dao.model.sql.AdminProjectTableEntity;
import org.thingsboard.server.dao.model.sql.CertenTableEntity;

import java.io.Serializable;
import java.util.List;

@Data
public class AdminResponse implements Serializable {
    private static final long serialVersionUID = 5541789721369995697L;

    /**
     * 返回项目的集合
     */
    private List<AdminProjectTableEntity> adList;

    /***
     * 成功标识
     */
    private Boolean flag;


    /***
     * 中间点位的数据
     */
    private CertenTableEntity certenTable;
}
