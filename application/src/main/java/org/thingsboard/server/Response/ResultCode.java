package org.thingsboard.server.Response;

/**
 * Create By IntelliJ IDEA
 *
 * <p>
 * 10000 -- 通用错误代码
 * 20000 -- 用户中心错误代码
 * </p>
 *
 * @author мr.тang
 * @date 2019/12/20 15:52
 * @email tangheng.java@outlook.com
 */
public interface ResultCode {

    /**
     * @Description 操作是否成功, true为成功，false操作失败
     * @Date 2019/12/20 16:02
     */
    boolean success();


    /**
     * @Description 操作代码
     * @Date 2019/12/20 16:02
     */
    int code();


    /**
     * @Description 提示信息
     * @Date 2019/12/20 16:02
     */
    String message();

}
