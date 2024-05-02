package com.kone.bi_backend.model.dto.chart;

import com.baomidou.mybatisplus.annotation.TableField;
import com.kone.bi_backend.model.dto.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {

    private Long id;

    /**
     * 名称
     */
    private String chartName;

    /**
     * 创建日期
     */
    private String sortField;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 排序
     */
    private String sortOrder;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
