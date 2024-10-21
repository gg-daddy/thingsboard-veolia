package org.thingsboard.server.service.collection;

import org.thingsboard.server.Request.CollectionRequest;
import org.thingsboard.server.Response.CollReponse;
import org.thingsboard.server.Response.QueryPaginationResult;
import org.thingsboard.server.Response.ResponseResult;
import org.thingsboard.server.Response.SelectBean;

import java.io.IOException;
import java.util.List;

/***
 *
 */
public interface CollectionServer {




    /**
     * 收藏获取不是收藏
     * @param collectionRequest
     */
    ResponseResult collection1(CollectionRequest collectionRequest, String userId, String phone) throws IOException;

    /***
     * 查询所有的
     * @return
     */
    public QueryPaginationResult findAll(String uuid);


    /***
     * 判断收藏
     * @param reponseList
     * @return
     */
    public List<CollReponse> collList1(List<CollReponse> reponseList, String userId, String phone);





    /***
     * pid传进来 cid出去
     * @param str
     * @return
     */
    public List<SelectBean> findSelect(String str, String uuid);

}
