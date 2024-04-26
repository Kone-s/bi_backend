package com.kone.bi_backend.model.dto.chart;

import java.io.Serializable;

import lombok.Data;

/**
 * 删除请求
 */
@Data
public class ChartDeleteRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
