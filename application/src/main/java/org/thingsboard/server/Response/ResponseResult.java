package org.thingsboard.server.Response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
public class ResponseResult implements Response {



    /**
     * @Field 操作是否成功
     */
    boolean success = SUCCESS;

    /**
     * @Field 操作代码
     */
    int code = SUCCESS_CODE;

    /**
     * @Field 提示信息
     */
    String message;

    public ResponseResult(ResultCode resultCode) {
        this.success = resultCode.success();
        this.code = resultCode.code();
        this.message = resultCode.message();
    }

    public static ResponseResult SUCCESS() {
        return new ResponseResult(CommonCode.SUCCESS);
    }

    

    public static ResponseResult CUSTOMIZE(ResultCode resultCode) {
        return new ResponseResult(resultCode);
    }

    public static ResponseResult FAIL() {
        return new ResponseResult(CommonCode.FAIL);
    }

}
