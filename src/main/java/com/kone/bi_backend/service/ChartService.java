package com.kone.bi_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kone.bi_backend.model.dto.chart.ChartQueryRequest;
import com.kone.bi_backend.model.entity.Chart;

/**
 * 可视图服务类
 */
public interface ChartService extends IService<Chart> {

    /**
     * 查询图表搜条件是否相同
     *
     * @param page1 当前页
     * @param page2 显示页数
     * @return
     */
    boolean isSamePage(Page<Chart> page1, Page<Chart> page2);

    /**
     * 查询图表是否相同
     *
     * @param chart1
     * @param chart2
     * @return
     */
    boolean isSameChart(Chart chart1, Chart chart2);


    /**
     * 获取查询条件
     *
     * @param chartQueryRequest
     * @return
     */
    QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);

}
