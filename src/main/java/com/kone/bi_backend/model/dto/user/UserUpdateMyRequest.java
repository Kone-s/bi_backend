package com.kone.bi_backend.model.dto.user;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

/**
 * 用户更新个人信息请求
 */
@Data
public class UserUpdateMyRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户电话
     */
    private String userPhone;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
