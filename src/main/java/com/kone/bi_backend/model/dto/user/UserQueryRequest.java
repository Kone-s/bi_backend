package com.kone.bi_backend.model.dto.user;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableField;
import com.kone.bi_backend.model.dto.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户邮箱
     */
    private String userEmail;

    /**
     * 用户角色：user/admin/ban
     */
    private String userRole;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
