package org.thingsboard.server.service.workOrder;/*
 * @Author:${zhangrui}
 * @Date:2020/9/16 17:56
 */

import org.thingsboard.server.Request.DealWorkOrderRequest;
import org.thingsboard.server.Request.WorkOrderRequest;
import org.thingsboard.server.Response.QueryPaginationResult;
import org.thingsboard.server.Response.ResponseResult;

public interface WorkOrderServer {


    void add(WorkOrderRequest workOrderRequest);

    /**
     * 查询的接口
     * @param pageNo
     * @param pageSize
     * @return
     */
    QueryPaginationResult findAll(String pageNo, String pageSize, String tenantId);


    /***
     * 添加维修单信息
     *
     */
    Boolean addWorkOrder(String equipmentNumber, String tenantId, String value, String type);


    /***
     * 工单的自动处理
     * 缺少了一个标准
     * @return
     */
    ResponseResult workOrderUpdate(String equipmentNumber, String type, String tenantId);

    /**
     * 工单处理
     * @param dealWorkOrderRequest
     * @return
     */
    ResponseResult dealOrder(DealWorkOrderRequest dealWorkOrderRequest);


}
