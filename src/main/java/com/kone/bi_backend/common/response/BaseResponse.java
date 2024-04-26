package com.kone.bi_backend.common.response;

import java.io.Serializable;

import lombok.Data;

/**
 * 通用返回类
 */
@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String msg;

    public BaseResponse(int code, T data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMsg());
    }
}
