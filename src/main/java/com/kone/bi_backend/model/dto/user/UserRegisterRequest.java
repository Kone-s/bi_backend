package com.kone.bi_backend.model.dto.user;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

/**
 * 用户注册请求体
 */
@Data
public class UserRegisterRequest implements Serializable {

    private String nickname;

    private String userEmail;

    private String userPassword;

    private String checkPassword;

    private String captcha;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
