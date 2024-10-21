package org.thingsboard.server.service.workOrder;/*
 * @Author:${zhangrui}
 * @Date:2020/9/16 17:58
 */


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.thingsboard.server.Request.DealWorkOrderRequest;
import org.thingsboard.server.Request.WorkOrderRequest;
import org.thingsboard.server.Response.CommonCode;
import org.thingsboard.server.Response.QueryPaginationResult;
import org.thingsboard.server.Response.ResponseResult;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.model.sql.WorkOrderEntity;
import org.thingsboard.server.dao.sql.device.DeviceRepository;
import org.thingsboard.server.dao.sql.workOrder.WorkOrderRepository;
import org.thingsboard.server.utils.DateUtils;
import org.thingsboard.server.utils.SnowflakeIdWorker;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DefaultWorkOrderServer implements WorkOrderServer {

    @Autowired
    private WorkOrderRepository workOrderRepository;



    @Autowired
    DeviceRepository deviceRepository;


    /***
     * 添加工单的逻辑
     *
     * @param workOrderRequest
     */
    @Override
    public void add(WorkOrderRequest workOrderRequest) {

        WorkOrderEntity workOrderEntity = new WorkOrderEntity();

        workOrderEntity.setId(UUID.randomUUID().toString());

        /**
         * 封装申请内容，申请日期，工单状态，操作者
         */
        workOrderEntity.setApplicationContent(workOrderRequest.getApplicationContent());
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        workOrderEntity.setDate(date);
        workOrderEntity.setOperator(workOrderRequest.getOperator());
        workOrderEntity.setStatus(0);
        workOrderEntity.setEquipmentNumber(workOrderRequest.getEquipmentNumber());
        workOrderEntity.setPhone(workOrderRequest.getPhone());
        log.info(new Date() + "添加一个维修单");
        workOrderRepository.save(workOrderEntity);

    }

    /**
     * 查询所有
     *
     * @return
     */
    @Override
    public QueryPaginationResult findAll(String pageNo, String pageSize, String tenantId) {

        Sort.Order order = new Sort.Order(Sort.Direction.DESC, "date");
        Sort.Order order1 = new Sort.Order(Sort.Direction.ASC, "status");

        List<Sort.Order> list = new ArrayList<>();
        list.add(order1);
        list.add(order);
        Sort sort1 = Sort.by(list);

        Pageable pageable = PageRequest.of(Integer.valueOf(pageNo), Integer.valueOf(pageSize), sort1);
        Specification<WorkOrderEntity> query = new Specification<WorkOrderEntity>() {
            @Override
            public Predicate toPredicate(Root<WorkOrderEntity> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<>();

                predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));

                return criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        };
        /**
         * 返回
         */
        Page<WorkOrderEntity> entityPage = workOrderRepository.findAll(query, pageable);
        QueryPaginationResult queryPaginationResult = new QueryPaginationResult();
        queryPaginationResult.setTotal(entityPage.getTotalElements());
        queryPaginationResult.setPageSize(Integer.valueOf(pageSize));
        queryPaginationResult.setTotalPage(entityPage.getTotalPages());

        /***
         * 工单内容  --- 设备号装换
         */
        List<WorkOrderEntity> entityList = this.change(entityPage.getContent());
        queryPaginationResult.setPage(entityList);
        return queryPaginationResult;
    }


    /***
     * 数据装换
     * @return
     */
    private List<WorkOrderEntity> change(List<WorkOrderEntity> list) {
        List<String> idList = list.stream().map(WorkOrderEntity::getEquipmentNumber).collect(Collectors.toList());
        List<DeviceEntity> deviceEntityList = deviceRepository.findByIdIn(idList);

        /***
         * 双循环过滤
         */
        for (int i = 0; i < list.size(); i++) {
            for (int j = 0; j < deviceEntityList.size(); j++) {
                if (list.get(i).getEquipmentNumber().equals(deviceEntityList.get(j).getStrId())) {
                    list.get(i).setEquipmentNumber(deviceEntityList.get(j).getLabel());
                    break;
                }
            }
        }
        return list;
    }


    /***
     * rpc自动添加
     * @param equipmentNumber
     * @param tenantId
     * @param value
     * @param type
     * @return
     */
    @Override
    public Boolean addWorkOrder(String equipmentNumber, String tenantId, String value, String type) {
        WorkOrderEntity workOrderEntity = new WorkOrderEntity();
        SnowflakeIdWorker snowflakeIdWorker = new SnowflakeIdWorker(1, 1);
        workOrderEntity.setId(String.valueOf(snowflakeIdWorker.nextId()));
        workOrderEntity.setDate(DateUtils.getNow());
        workOrderEntity.setStatus(0);
        workOrderEntity.setType(type);
        workOrderEntity.setTenantId(tenantId);
        DeviceEntity deviceEntity = deviceRepository.findByTenantIdAndName(tenantId, equipmentNumber);
        workOrderEntity.setEquipmentNumber(deviceEntity.getStrId());
        StringBuilder sb = new StringBuilder("<span style='color:#F70606'>"+deviceEntity.getLabel()+"</span>");
        sb.append("检测到").append("<span style='color:#F70606'>"+type+"</span>")
                .append("出现指标异常，其正常值在").append(getStr(type)).append("之间,").append("检测值为")
                .append("<span style='color:#F70606'>"+value+"</span>").append("请及时处理");
        workOrderEntity.setApplicationContent(sb.toString());
        workOrderRepository.save(workOrderEntity);
        log.info("出现异常，rpc调用新增工单");
        return true;
    }


    /****
     *
     * @param equipmentNumber 设备号
     * @param type            报警类型
     * @param tenantId        租户的id
     * @return
     */
    @Override
    public ResponseResult workOrderUpdate(String equipmentNumber, String type, String tenantId) {
        DeviceEntity deviceEntity = deviceRepository.findByTenantIdAndName(tenantId, equipmentNumber);
        List<WorkOrderEntity> list = workOrderRepository.findByEquipmentNumberAndTypeAndTenantIdOrderByDateDesc(deviceEntity.getStrId(), type, tenantId);
        DealWorkOrderRequest dealWorkOrderRequest = new DealWorkOrderRequest() ;
        dealWorkOrderRequest.setDealContent("自动消失");
        dealWorkOrderRequest.setOperator("系统");
        dealWorkOrderRequest.setPhone("");
        dealWorkOrderRequest.setId(list.get(0).getId());
        return dealOrder(dealWorkOrderRequest);
    }

    /***
     * 处理工单的逻辑
     * @param dealWorkOrderRequest
     * @return
     */
    @Override
    public ResponseResult dealOrder(DealWorkOrderRequest dealWorkOrderRequest) {


        String strSpan = "<span style='color:#F70606'>";
        String strSpanEnd = "</span>";

        WorkOrderEntity workOrderEntity = workOrderRepository.findById(dealWorkOrderRequest.getId()).get();

        /***
         * 状态改变  1为以处理了
         */
        workOrderEntity.setStatus(1);
        workOrderEntity.setPhone(dealWorkOrderRequest.getPhone());
        workOrderEntity.setDealDate(DateUtils.getNow());
        workOrderEntity.setDealContent(dealWorkOrderRequest.getDealContent());
        workOrderEntity.setOperator(dealWorkOrderRequest.getOperator());
        String str = workOrderEntity.getApplicationContent().replaceAll(strSpan, "").replaceAll(strSpanEnd, "");
        workOrderEntity.setApplicationContent(str);
        workOrderRepository.save(workOrderEntity);

        return ResponseResult.CUSTOMIZE(CommonCode.DEAL_SUCCESS);
    }


    /***
     * 获取到正常值
     * @return
     */
    private String getStr(String type) {
        switch (type) {
            case "温度":
                return WorkOrderEnum.TEMPERATURE.getName();
            case "湿度":
                return WorkOrderEnum.HUMIDITY.getName();
            case "PM2.5":
                return WorkOrderEnum.PM25.getName();
            case "PM10":
                return WorkOrderEnum.PM10.getName();
            case "PM1.0":
                return WorkOrderEnum.PM100.getName();
            case "甲醛":
                return WorkOrderEnum.CH20.getName();
            case "二氧化碳":
                return WorkOrderEnum.CO2.getName();
            default:
                return "";


        }
    }


}
