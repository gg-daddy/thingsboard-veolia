package org.thingsboard.server.Response;

import lombok.Data;

import java.io.Serializable;

@Data
public class CollectionResponse implements Serializable {
    private static final long serialVersionUID = 7768340310630782838L;

    /**
     * 是不是收藏了
     */
    private Boolean flag = false;

    /**
     *  pm2.5的值
     */
    private String pm25;


    /**
     * 温度
     */
    private String template;


    /***
     * 湿度
     */
    private String humidly;

}
