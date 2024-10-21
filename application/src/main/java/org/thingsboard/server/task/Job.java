package org.thingsboard.server.task;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.utils.DateUtils;

/**
 * Create By IntelliJ IDEA
 *
 * @author мr.тang
 * @date 2020/1/9 11:50
 * @email tangheng.java@outlook.com
 */
@Slf4j
public abstract class Job {

     /**
     * @Description 开始任务
     * @Date 2020/1/9 11:55
     */
    void beginTasks() {
        log.info("进入定时任务！DateTime：{}", DateUtils.getDate(System.currentTimeMillis()));
    }

    /**
     * @Description 处理中
     * @Date 2020/1/9 12:00
     */
    void processingTasks() throws Exception {

    }

    /**
     * @Description 结束任务
     * @Date 2020/1/9 11:55
     */
    void endTasks( String id) {
        log.info("计算完成，数据日期:设备", id);
    }
}
