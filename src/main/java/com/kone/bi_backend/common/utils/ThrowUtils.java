package com.kone.bi_backend.common.utils;

import com.kone.bi_backend.common.exception.CustomizeException;
import com.kone.bi_backend.common.response.ErrorCode;

/**
 * 抛异常工具类
 */
public class ThrowUtils {

    /**
     * 条件成立则抛异常
     *
     * @param condition
     * @param runtimeException
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件成立则抛异常
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new CustomizeException(errorCode));
    }

    /**
     * 条件成立则抛异常
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String msg) {
        throwIf(condition, new CustomizeException(errorCode, msg));
    }
}
