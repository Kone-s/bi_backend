package com.kone.bi_backend.model.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.kone.bi_backend.model.entity.User;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author Kone
 * @Date 2024/5/2
 */
@Data
public class PageUser implements Serializable {

    private User user;

    /**
     * 总积分
     */
    private Long scoreTotal;

    /**
     * 用户消耗tokens
     */
    private Long tokens;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public PageUser(User user, Long score) {
        this.user = user;
        this.scoreTotal = score;
    }
}
