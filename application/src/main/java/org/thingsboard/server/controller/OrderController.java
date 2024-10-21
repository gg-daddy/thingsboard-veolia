package org.thingsboard.server.controller;
/*
 * @Author:${zhangrui}
 * @Date:2020/9/15 14:21
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.Request.DealWorkOrderRequest;
import org.thingsboard.server.Request.WorkOrderRequest;
import org.thingsboard.server.Response.QueryPaginationResult;
import org.thingsboard.server.Response.ResponseResult;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.dao.model.ParamConstants;
import org.thingsboard.server.dao.model.sql.WorkOrderEntity;
import org.thingsboard.server.service.workOrder.WorkOrderServer;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

@RestController
@RequestMapping("/api/noauth")
public class OrderController extends BaseController {

    @Autowired
    private WorkOrderServer workOrderServer;

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    @ResponseBody
    public Boolean add(@RequestBody WorkOrderRequest workOrderRequest) {

        workOrderServer.add(workOrderRequest);
        return true;
    }

    @RequestMapping(value = "/findAll", method = RequestMethod.GET)
    @ResponseBody
    public QueryPaginationResult<WorkOrderEntity> findAll(@RequestParam("pageNo") String pageNo,
                                                          @RequestParam("pageSize") String pageSize, @RequestParam("tenantId") String tenantId) {
        return workOrderServer.findAll(pageNo,pageSize,tenantId);

    }

    /***
     * 生成工单接口
     * 无法获取到权限
     * @return
     */
    @RequestMapping(value = "/rpcAdd", method = RequestMethod.POST)
    @ResponseBody
    public Boolean receice(HttpServletRequest request) throws ThingsboardException, UnsupportedEncodingException {


        request.setCharacterEncoding("UTF-8");


        /**
         *   设备编号
         */
       String equipmentNumber =  request.getHeader(ParamConstants.equipmentNumber);

        /**
         * 租户的id
         */
        String tenantId = request.getHeader(ParamConstants.tenantId);

        /**
         * 报警类型
         */
        String value = request.getHeader(ParamConstants.value);

        /**
         * 类型
         */
        String type = request.getHeader(ParamConstants.type);

        type = new String(type.getBytes("ISO8859-1"),"UTF-8");


        equipmentNumber = new String(equipmentNumber.getBytes("ISO8859-1"),"UTF-8");

        return  workOrderServer.addWorkOrder(equipmentNumber,tenantId,value,type);
    }


    /***
     * 工单自动更新
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/orderOk", method = RequestMethod.POST)
    @ResponseBody
    public ResponseResult orderOk(HttpServletRequest request) throws Exception {


        request.setCharacterEncoding("UTF-8");


        /**
         *   设备编号
         */
        String equipmentNumber =  request.getHeader(ParamConstants.equipmentNumber);

        /**
         * 租户的id
         */
        String tenantId = request.getHeader(ParamConstants.tenantId);
        /**
         * 类型
         */
        String type = request.getHeader(ParamConstants.type);

        type = new String(type.getBytes("ISO8859-1"),"UTF-8");


        equipmentNumber = new String(equipmentNumber.getBytes("ISO8859-1"),"UTF-8");


      return   workOrderServer.workOrderUpdate(equipmentNumber,type,tenantId);

    }

    /***
     * 处理接口
     * @param
     * @return
     */
    @RequestMapping(value = "/deal", method = RequestMethod.POST)
    @ResponseBody
    public ResponseResult update(@RequestBody DealWorkOrderRequest dealWorkOrderRequest){

        return workOrderServer.dealOrder(dealWorkOrderRequest);

    }


}
