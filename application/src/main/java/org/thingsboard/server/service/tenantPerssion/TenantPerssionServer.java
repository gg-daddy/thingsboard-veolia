package org.thingsboard.server.service.tenantPerssion;


import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.Response.MessBean;
import org.thingsboard.server.Response.QueryPaginationResult;
import org.thingsboard.server.Response.UserBean;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.dao.model.sql.AdminMannerEntity;
import org.thingsboard.server.dao.model.sql.TenantPerssionChangeTableEntity;
import org.thingsboard.server.dao.model.sql.TenantPerssionTableEntity;
import org.thingsboard.server.dao.sql.adminManner.AdminMannerRepository;
import org.thingsboard.server.dao.sql.tenantPerssion.TenantPerssionTableRepository;
import org.thingsboard.server.dao.sql.tenantPerssionChange.TenantPerssionChangeRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TenantPerssionServer {

    @Autowired
    private TenantPerssionTableRepository tenantPerssionTableRepository;

    @Autowired
    private TenantPerssionChangeRepository tenantPerssionChangeRepository;

    @Autowired
    AdminMannerRepository adminMannerRepository;

    /***
     * 根据电话号来确定是不是有权限来登录
     *
     * 根据数据返回的不同区别不同的地方
     * @return
     */
    public QueryPaginationResult getPerssion(String phone) {

        AdminMannerEntity mannerEntity = adminMannerRepository.findByPhone(phone);
        /***
         * 代表不在管理员表里
         */
        QueryPaginationResult queryPaginationResult = new QueryPaginationResult();
        if (mannerEntity == null) {
            Optional<TenantPerssionTableEntity> tableEntity = tenantPerssionTableRepository.findByPhone(phone);
            if (tableEntity.isPresent()) {
                queryPaginationResult.setFlag(true);
                queryPaginationResult.setObject(phone);
                queryPaginationResult.setMess(getUserBean(tableEntity.get().getTenantId(), false));

                List<MessBean> list = tenantPerssionChangeRepository.findByTenantId(tableEntity.get().getTenantId()).stream().map(p -> {
                    MessBean messBean = new MessBean();
                    messBean.setTenantId(p.getTenantId());
                    messBean.setTenantName(p.getProjectName());
                    return messBean;
                }).collect(Collectors.toList());
                queryPaginationResult.setList(list);
                return queryPaginationResult;
            } else {
                /**
                 * 无权限登录
                 */
                queryPaginationResult.setFlag(false);
                return queryPaginationResult;
            }
        } else {
            queryPaginationResult.setFlag(true);
            queryPaginationResult.setObject(phone);
            queryPaginationResult.setAdmin(true);
            queryPaginationResult.setMess(getUserBean(mannerEntity.getTenantId(), true));

            List<MessBean> list = tenantPerssionChangeRepository.findAll().stream().map(p -> {
                MessBean messBean = new MessBean();
                messBean.setTenantId(p.getTenantId());
                messBean.setTenantName(p.getProjectName());
                return messBean;
            }).collect(Collectors.toList());

            queryPaginationResult.setList(list);
            return queryPaginationResult;
        }
    }


    public UserBean getUserBean(String teantId, Boolean flag) {
        List<TenantPerssionChangeTableEntity> entityList = tenantPerssionChangeRepository.findByTenantId(teantId);
        /***
         * 返回的bean
         */
        TenantPerssionChangeTableEntity t = entityList.get(0);

        RestTemplate restTemplate = new RestTemplate();
        String url = "http://127.0.0.1:9005/api/auth/login";
        Map<String, String> map = new HashMap<>();
        map.put("username", t.getUsername());
        map.put("password", t.getPassword());
        HttpEntity<Map> entity = new HttpEntity<Map>(map, null);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
        String user = responseEntity.getBody();
        UserBean bean = JSON.parseObject(user, UserBean.class);
        bean.setRenantName(flag ? t.getProjectName() : ("1eab9e8ddca861092284b8ee4a402d9".equals(teantId) ? "荟中心" : t.getProjectName()));
        bean.setTenantId(t.getTenantId());
        bean.setSumId(t.getSumId());
        return bean;
    }


    /**
     *
     * @param teantId
     * @param flag
     * @return
     */
    public UserBean getUserBeanForChangeUser(String teantId, String name, Boolean flag) {

        TenantPerssionChangeTableEntity t = new TenantPerssionChangeTableEntity();

        if(!"1eab9e8ddca861092284b8ee4a402d9".equals(teantId)){
            List<TenantPerssionChangeTableEntity> entityList = tenantPerssionChangeRepository.findByTenantId(teantId);
            t = entityList.get(0);
        }else{
            t = tenantPerssionChangeRepository.findByProjectName(name);
            if(t == null){
                List<TenantPerssionChangeTableEntity> entityList = tenantPerssionChangeRepository.findByTenantId(teantId);
                t = entityList.get(0);
            }
        }

        RestTemplate restTemplate = new RestTemplate();
        String url = "http://127.0.0.1:9005/api/auth/login";
        Map<String, String> map = new HashMap<>();
        map.put("username", t.getUsername());
        map.put("password", t.getPassword());
        HttpEntity<Map> entity = new HttpEntity<Map>(map, null);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
        String user = responseEntity.getBody();
        UserBean bean = JSON.parseObject(user, UserBean.class);
        bean.setRenantName(flag ? t.getProjectName() : ("1eab9e8ddca861092284b8ee4a402d9".equals(teantId) ? "荟中心" : t.getProjectName()));
        bean.setTenantId(t.getTenantId());
        bean.setSumId(t.getSumId());
        return bean;
    }



}
