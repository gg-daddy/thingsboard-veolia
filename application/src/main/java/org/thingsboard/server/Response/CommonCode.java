package org.thingsboard.server.Response;

import lombok.ToString;

/**
 * Create By IntelliJ IDEA
 *
 * @author мr.тang
 * @date 2019/12/20 15:52
 * @email tangheng.java@outlook.com
 */
@ToString
public enum CommonCode implements ResultCode {
    INVALID_PARAM(false, 10003, "非法操作！"),
    SUCCESS(true, 10000, "操作成功！"),
    FAIL(false, 11111, "失败！"),

    UNAUTHENTICATED(false, 10001, "此操作需要登陆系统！"),
    UNAUTHORISE(false, 10002, "权限不足，无权操作！"),
    SERVER_ERROR(false, 99999, "抱歉，系统繁忙，请稍后重试！"),

    DEAL_SUCCESS(true, 88, "成功处理"),
    COLLECTION_YES_SUCCESS(true,81,"收藏成功"),
    COLLECTION_NO_SUCCESS(true,82,"取消收藏成功");
    /**
     * @Field 操作是否成功
     */
    boolean success;

    /**
     * @Field 操作代码
     */
    int code;

    /**
     * @Field提示信息
     */
    String message;

    private CommonCode(boolean success, int code, String message) {
        this.success = success;
        this.code = code;
        this.message = message;
    }

    @Override
    public boolean success() {
        return success;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }


}
