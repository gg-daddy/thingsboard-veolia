package org.thingsboard.server.controller;/*
 * @Author:${zhangrui}
 * @Date:2020/9/23 17:35
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.Request.FeedbackRequest;
import org.thingsboard.server.Response.CommonCode;
import org.thingsboard.server.Response.QueryPaginationResult;
import org.thingsboard.server.Response.ResponseResult;
import org.thingsboard.server.dao.model.sql.FeedbackEntity;
import org.thingsboard.server.service.feedback.FeedbackServer;

@RestController
@RequestMapping("/api/noauth/feedback")
public class FeedbackController extends BaseController {


    @Autowired
    private FeedbackServer feedbackServer;

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    @ResponseBody
    public ResponseResult add(@RequestBody FeedbackRequest feedbackRequest) {

        feedbackServer.add(feedbackRequest);
        return ResponseResult.CUSTOMIZE(CommonCode.SUCCESS);

    }



    @RequestMapping(value = "/findAll", method = RequestMethod.GET)
    @ResponseBody
    public QueryPaginationResult<FeedbackEntity> findAll(@RequestParam("pageNo") String pageNo,
                                                         @RequestParam("pageSize") String pageSize) {
        return feedbackServer.findAll(pageNo,pageSize);

    }

}
