package com.kone.bi_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kone.bi_backend.common.constant.CommonConstant;
import com.kone.bi_backend.common.exception.CustomizeException;
import com.kone.bi_backend.common.response.ErrorCode;
import com.kone.bi_backend.common.utils.SqlUtils;
import com.kone.bi_backend.mapper.ChartMapper;
import com.kone.bi_backend.model.dto.chart.ChartQueryRequest;
import com.kone.bi_backend.model.entity.Chart;
import com.kone.bi_backend.service.ChartService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 可视图实现类
 */
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart> implements ChartService {
    public boolean isSamePage(Page<Chart> page1, Page<Chart> page2) {
        // 检查两个分页对象是否为null
        if (page1 == null || page2 == null) {
            return false;
        }
        // 检查总页数是否相同
        if (page1.getPages() != page2.getPages()) {
            return false;
        }
        // 检查当前页数是否相同
        if (page1.getCurrent() != page2.getCurrent()) {
            return false;
        }
        // 检查每页大小是否相同
        if (page1.getSize() != page2.getSize()) {
            return false;
        }
        // 检查数据项是否相同
        List<Chart> list1 = page1.getRecords();
        List<Chart> list2 = page2.getRecords();
        // 如果数据项个数不同，则两个分页对象不同
        if (list1.size() != list2.size()) {
            return false;
        }
        // 检查每个数据项是否相同
        for (int i = 0; i < list1.size(); i++) {
            Chart chart1 = list1.get(i);
            Chart chart2 = list2.get(i);
            // 检查数据项是否相同，可以根据具体业务需求来定义
            if (!isSameChart(chart1, chart2)) {
                return false;
            }
        }
        return true;
    }

    // 检查两个图表对象是否相同，可以根据具体业务需求来定义
    public boolean isSameChart(Chart chart1, Chart chart2) {
        // 比较图表对象的各个属性是否相同，例如ID、名称、类型等等
        // 如果所有属性都相同，则返回true；否则返回false
        return chart1.getId().equals(chart2.getId())
                && chart1.getChartStatus().equals(chart2.getChartStatus());
    }

    @Override
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        if (chartQueryRequest == null) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String chartName = chartQueryRequest.getChartName();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartName), "chart_name", chartName);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chart_type", chartType);
        queryWrapper.eq(userId != null && userId > 0, "user_id", userId);
        queryWrapper.eq("is_delete", 0);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}




