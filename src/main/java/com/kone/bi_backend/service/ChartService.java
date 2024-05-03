package com.kone.bi_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kone.bi_backend.model.dto.chart.ChartQueryRequest;
import com.kone.bi_backend.model.entity.Chart;

/**
 * 可视图服务类
 */
public interface ChartService extends IService<Chart> {
    /**
     * 获取查询条件
     *
     * @param chartQueryRequest
     * @return
     */
    QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);

}
