package org.thingsboard.server.Response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Create By IntelliJ IDEA
 *
 * @author мr.тang
 * @date 2019/12/20 15:52
 * @email tangheng.java@outlook.com
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class QueryPaginationResult<T> {


    private Boolean flag;

    /**
     * 数据列表
     */
    private List<T> page;

    /**
     * 数据总数
     */
    private Long total;


    /**
     * 每页数
     */
    private Integer pageSize;

    /***
     * 总页码数
     */
    private Integer totalPage;



    private Object object;


    public Object mess;

    /**
     * 登录认证
     */
    private String tokenAdmin;


    public boolean isAdmin  = false;


    public List<MessBean> list;


    public String sumId;
}
