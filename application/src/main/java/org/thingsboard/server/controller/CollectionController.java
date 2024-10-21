package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.Request.CollectionRequest;
import org.thingsboard.server.Request.IndexRequest;
import org.thingsboard.server.Request.NumberRequest;
import org.thingsboard.server.Response.*;
import org.thingsboard.server.cassandra.TsKvLatestcfEntity;
import org.thingsboard.server.cassandra.TsKvServer;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.collection.CollectionServer;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Slf4j
public class CollectionController extends BaseController {


    @Autowired
    private CollectionServer collectionServer;

    @Autowired
    private TsKvServer tsKvServer;


    /**
     * 权限只有用户可以
     * 取消收藏是false
     * 收藏是true
     *
     * @param collectionRequest
     * @return
     * @throws ThingsboardException
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/collection/coll1", method = RequestMethod.POST)
    public ResponseResult collection1(@RequestBody CollectionRequest collectionRequest) throws ThingsboardException, IOException {
        SecurityUser securityUser = getCurrentUser();
        return collectionServer.collection1(collectionRequest, securityUser.getId().toString(), collectionRequest.getPhone());
    }


    /***
     * 查询全部的
     * @return
     * @throws ThingsboardException
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/collection/findAll1", method = RequestMethod.POST)
    public QueryPaginationResult findAll1(@RequestBody NumberRequest numberRequest) throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        String uuid = UUIDConverter.fromTimeUUID(UUID.fromString(securityUser.getTenantId().getId().toString()));
        QueryPaginationResult result = collectionServer.findAll(uuid);
        result.setPage(collectionServer.collList1(result.getPage(), securityUser.getId().toString(), numberRequest.getPhone()));
        return result;
    }


    /***
     * 查询全部收藏的
     * @return
     * @throws ThingsboardException
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/collection/findAllColl1", method = RequestMethod.POST)
    public QueryPaginationResult findAllColl1(@RequestBody NumberRequest numberRequest) throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        String uuid = UUIDConverter.fromTimeUUID(UUID.fromString(securityUser.getTenantId().getId().toString()));
        QueryPaginationResult result = collectionServer.findAll(uuid);
        List<CollReponse> list = collectionServer.collList1(result.getPage(), securityUser.getId().toString(), numberRequest.getPhone());
        result.setPage(getColl(list));
        return result;
    }


    /**
     * 获取收藏列表
     *
     * @return
     */
    private List<CollReponse> getColl(List<CollReponse> list) {
        return list.stream().filter(CollReponse -> CollReponse.getFlag()).collect(Collectors.toList());
    }


    /**
     * 查询天气
     *
     * @return
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/findByWeather", method = RequestMethod.GET)
    public WeatherResponse findByWeather() {
        SecurityUser securityUser = (SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return tsKvServer.findWetather(securityUser.getName());
    }


    /**
     * 详情打开
     *
     * @return
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/findForDetail", method = RequestMethod.POST)
    public List<Object> findForDetailLimit3(@RequestBody CollectionRequest collectionRequest) {
        return tsKvServer.findForDetailLimit1(UUID.fromString(collectionRequest.getDeviceIds()), collectionRequest.getType());

    }

    /***
     * 获取下拉菜单
     * @return
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/findSelect", method = RequestMethod.POST)
    public QueryPaginationResult findSelect(@RequestBody CollectionRequest collectionRequest) throws ThingsboardException {

        SecurityUser securityUser = getCurrentUser();
        String uuid = UUIDConverter.fromTimeUUID(UUID.fromString(securityUser.getTenantId().getId().toString()));
        List<SelectBean> list = collectionServer.findSelect(collectionRequest.getDeviceIds(), uuid);
        QueryPaginationResult result = new QueryPaginationResult();
        result.setPage(list);
        return result;
    }


    /***
     * 警报 PM2.5
     *
     * @return
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/findWarnList", method = RequestMethod.POST)
    public List<Object> findWarnList(@RequestBody Map<String, Object> map) throws ThingsboardException {
        if (map.get("projectName") != null && !"".equals(map.get("projectName").toString())) {
            return tsKvServer.getWarnListForName(map.get("projectName").toString());
        } else {
            SecurityUser securityUser = getCurrentUser();
            String uuid = UUIDConverter.fromTimeUUID(UUID.fromString(securityUser.getTenantId().getId().toString()));
            return tsKvServer.getWarnList(uuid);
        }
    }

    /***
     * 查询最新的消息
     * @return
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/findNew", method = RequestMethod.POST)
    public NewResponse findNew(@RequestBody Map<String, Object> map) throws ThingsboardException {
        if (map.get("projectName") != null && !"".equals(map.get("projectName").toString())) {
            return tsKvServer.getPm25NewForName(map.get("projectName").toString());
        } else {
            SecurityUser securityUser = getCurrentUser();
            String uuid = UUIDConverter.fromTimeUUID(UUID.fromString(securityUser.getTenantId().getId().toString()));
            return tsKvServer.getPm25New(uuid);
        }
    }


    /***
     * 首页打开的时候
     * @return
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/indexOpen", method = RequestMethod.POST)
    public QueryResult indexOpen(@RequestBody IndexRequest indexRequest) {
        QueryResult queryResult = new QueryResult();
        try {
            List<TsKvLatestcfEntity> latestcfEntityList = tsKvServer.findByList(UUID.fromString(indexRequest.getSumId()));
            Map<String, List<List<Long>>> stringListMap = tsKvServer.getIndexOpenResponses(latestcfEntityList);
            queryResult.setFlag(true);
            queryResult.setData(stringListMap);
        } catch (Exception e) {
            queryResult.setFlag(false);
        }
        return queryResult;
    }


    public static void main(String[] args) {




    }

}
