package com.kone.bi_backend.model.dto.chart;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传请求
 */
@Data
public class GenChartByAiRequest implements Serializable {

    /**
     * 名称
     */
    private String chartName;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
