package org.thingsboard.server.Response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CollReponse implements Serializable {
    private static final long serialVersionUID = -6606781117277841971L;

    /**
     * pg库的id
     */
    private List<String> pid;

    /**
     * cassandra库的id
     */
    private List<String> cid;

    /**
     * 所处楼层
     */
    private String name;


    private String labal;

    /**
     * 是不是被收藏
     * false: 未被收藏
     * true:  被收藏了
     */
    public Boolean flag = false;


    private  double pM25 = 7;

    private double template = 22;

    private double humidly = 33;

    private double CO2 = 520;


}
