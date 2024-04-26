package com.kone.bi_backend.model.dto.user;

import java.io.Serializable;

import lombok.Data;

/**
 * 用户登录请求
 */
@Data
public class UserLoginRequest implements Serializable {

    private String userEmail;

    private String userPassword;
}
