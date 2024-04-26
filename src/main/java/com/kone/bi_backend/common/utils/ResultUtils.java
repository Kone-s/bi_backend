package com.kone.bi_backend.common.utils;

import com.kone.bi_backend.common.response.BaseResponse;
import com.kone.bi_backend.common.response.ErrorCode;

/**
 * 返回工具类
 */
public class ResultUtils {

    /**
     * 成功
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    /**
     * 失败
     */
    public static BaseResponse error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败
     */
    public static BaseResponse error(int code, String msg) {
        return new BaseResponse(code, null, msg);
    }

    /**
     * 失败
     */
    public static BaseResponse error(ErrorCode errorCode, String msg) {
        return new BaseResponse(errorCode.getCode(), null, msg);
    }
}
