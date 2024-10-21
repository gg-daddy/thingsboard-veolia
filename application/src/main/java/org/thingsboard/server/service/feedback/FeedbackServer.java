package org.thingsboard.server.service.feedback;/*
 * @Author:${zhangrui}
 * @Date:2020/9/23 17:47
 */

import org.thingsboard.server.Request.FeedbackRequest;
import org.thingsboard.server.Request.WorkOrderRequest;
import org.thingsboard.server.Response.QueryPaginationResult;

public interface FeedbackServer {


    /***
     * 提交反馈
     * @param feedbackRequest
     */
    void add(FeedbackRequest feedbackRequest);


    /***
     * 查询
     * @param pageNo
     * @param pageSize
     * @return
     */
    QueryPaginationResult findAll(String pageNo, String pageSize);

}
