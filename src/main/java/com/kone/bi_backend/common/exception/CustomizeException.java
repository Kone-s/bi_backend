package com.kone.bi_backend.common.exception;

import com.kone.bi_backend.common.response.ErrorCode;
import lombok.Getter;

/**
 * 自定义异常类
 */
@Getter
public class CustomizeException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    public CustomizeException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public CustomizeException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    public CustomizeException(ErrorCode errorCode, String msg) {
        super(msg);
        this.code = errorCode.getCode();
    }

}
