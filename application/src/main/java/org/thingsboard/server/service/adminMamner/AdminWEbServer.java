package org.thingsboard.server.service.adminMamner;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.Request.WebUserRequest;
import org.thingsboard.server.Response.AdminResponse;
import org.thingsboard.server.Response.QueryPaginationResult;
import org.thingsboard.server.Response.UserBean;
import org.thingsboard.server.dao.model.sql.AdminProjectTableEntity;
import org.thingsboard.server.dao.model.sql.AdminTableEntity;
import org.thingsboard.server.dao.model.sql.CertenTableEntity;
import org.thingsboard.server.dao.sql.adminManner.AdminProjectTableRepository;
import org.thingsboard.server.dao.sql.adminManner.AdminTableRepository;
import org.thingsboard.server.dao.sql.adminManner.CertenTableRepository;
import org.thingsboard.server.utils.BCryptUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
@Slf4j
public class AdminWEbServer {


    @Autowired
    private AdminTableRepository tableRepository;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private AdminProjectTableRepository projectTableRepository;

    @Autowired
    private CertenTableRepository certenTableRepository;


    /***
     *  管理员登录
     *
     */
    public QueryPaginationResult webAdminLogin(Map<String, String> map) {

        Optional<AdminTableEntity> optional = tableRepository.findByUsername(map.get("username"));
        QueryPaginationResult result = new QueryPaginationResult();
        if (optional.isPresent()) {
            boolean flag = BCryptUtil.matches(map.get("password"), optional.get().getPassword());
            if (flag) {
                result.setFlag(flag);
                result.setObject(optional.get().getAimUrl());
                result.setTokenAdmin(optional.get().getAdminToken());
                return result;
            }
        }
        result.setFlag(false);
        result.setMess("用户名或密码错误");
        return result;


    }


    /***
     * 初始登录
     * @return
     */
    public String init() {
        AdminTableEntity tableEntity = tableRepository.findByUsername("root").get();
        return tableEntity.getErrUrl();
    }


    /**
     * 点位切换
     *
     * @param userRequest
     * @return
     */
    public QueryPaginationResult webChangeUser(WebUserRequest userRequest) {
        System.out.println(userRequest);

        RestTemplate restTemplate = new RestTemplate();
        String url = "http://127.0.0.1:9005/api/auth/login";
        Map<String, String> map = new HashMap<>();
        map.put("username", userRequest.getUsername());
        map.put("password", userRequest.getPassword());
        HttpEntity<Map> entity = new HttpEntity<Map>(map, null);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
        String user = responseEntity.getBody();
        QueryPaginationResult result = new QueryPaginationResult();
        result.setObject(JSON.parseObject(user, UserBean.class));
        return result;
    }

    /***
     *
     * @return
     */
    public AdminResponse initFindAll() {
        List<AdminProjectTableEntity> entityList = projectTableRepository.findAll();
        CertenTableEntity certenTable = certenTableRepository.findAll().get(0);

        AdminResponse adminResponse = new AdminResponse();
        if (entityList.size() != 0 && entityList != null) {
            adminResponse.setAdList(entityList);
            adminResponse.setFlag(true);
            adminResponse.setCertenTable(certenTable);
        } else {
            adminResponse.setAdList(entityList);
            adminResponse.setFlag(false);
        }
        return adminResponse;
    }

    public static void main(String[] args) {

//        String encode = BCryptUtil.encode("123456");
//        System.out.println(encode);

        System.out.println(UUID.randomUUID().toString().replaceAll("-", ""));
    }


}
