package com.kone.bi_backend.model.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * Bi 的返回结果
 */
@Data
public class BiResponseVO implements Serializable {

    private String genChart;

    private String genResult;

    private Long chartId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
