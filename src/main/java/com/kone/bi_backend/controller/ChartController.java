package com.kone.bi_backend.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kone.bi_backend.common.constant.CommonConstant;
import com.kone.bi_backend.common.exception.CustomizeException;
import com.kone.bi_backend.common.response.BaseResponse;
import com.kone.bi_backend.common.response.ErrorCode;
import com.kone.bi_backend.common.server.RedisCacheServer;
import com.kone.bi_backend.common.server.RedisLimiterServer;
import com.kone.bi_backend.common.server.SparkAIServer;
import com.kone.bi_backend.common.server.WebSocketServer;
import com.kone.bi_backend.common.utils.ExcelUtils;
import com.kone.bi_backend.common.utils.ResultUtils;
import com.kone.bi_backend.common.utils.SqlUtils;
import com.kone.bi_backend.common.utils.ThrowUtils;
import com.kone.bi_backend.model.dto.chart.ChartDeleteRequest;
import com.kone.bi_backend.model.dto.chart.ChartQueryRequest;
import com.kone.bi_backend.model.dto.chart.ChartUpdateRequest;
import com.kone.bi_backend.model.dto.chart.GenChartByAiRequest;
import com.kone.bi_backend.model.entity.Chart;
import com.kone.bi_backend.model.entity.User;
import com.kone.bi_backend.model.vo.BiResponseVO;
import com.kone.bi_backend.service.ChartService;
import com.kone.bi_backend.service.ScoreService;
import com.kone.bi_backend.service.UserService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.kone.bi_backend.common.constant.FileConstant.FILE_SIZE;

/**
 * 图表接口
 */
@RestController
@RequestMapping("/chart")
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private SparkAIServer sparkAIServer;

    @Resource
    private ScoreService scoreService;

    @Resource
    private RedisLimiterServer redisLimiterServer;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private RedisCacheServer redisCacheServer;

    @Resource
    private WebSocketServer webSocketServer;

    /**
     * 删除
     *
     * @param chartDeleteRequest 删除图表请求类
     * @param request            请求
     * @return 成功信息
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody ChartDeleteRequest chartDeleteRequest, HttpServletRequest request) {
        if (chartDeleteRequest == null || chartDeleteRequest.getId() <= 0) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = chartDeleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(user)) {
            throw new CustomizeException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新图标信息
     *
     * @param chartUpdateRequest 图标更新实体类
     * @param request            更新请求
     * @return
     */
    @PostMapping("/update/gen")
    public BaseResponse<Boolean> updateGenChart(@RequestBody ChartUpdateRequest chartUpdateRequest, HttpServletRequest request) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() == null || chartUpdateRequest.getId() <= 0) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        boolean result = chartService.updateById(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 分页获取当前用户创建的资源列表，并使用redis缓存
     *
     * @param chartQueryRequest 图表查询请求
     * @param request           请求
     * @return 成功信息
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest, HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        String cacheKey = "ChartController_listMyChartVOByPage_" + chartQueryRequest.getCurrent() + "_" + chartQueryRequest.getChartType() + "_" + chartQueryRequest.getChartName();
        Page<Chart> cachedChartPage = redisCacheServer.getCachedResult(cacheKey);
        // 判断是否缓存命中
        if (cachedChartPage != null) {
            // 缓存命中
            return ResultUtils.success(cachedChartPage);
        } else {
            // 缓存未命中，从数据库中查询数据，并放入缓存
            Page<Chart> chartPage = chartService.page(new Page<>(current, size), getQueryWrapper(chartQueryRequest));
            redisCacheServer.asyncPutCachedResult(cacheKey, chartPage);
            return ResultUtils.success(chartPage);
        }
    }


    /**
     * 智能分析（同步）
     *
     * @param multipartFile       文件流
     * @param genChartByAiRequest 图表参数
     * @param request             请求
     * @return 图表的信息
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponseVO> genChartByAi(@RequestPart("file") MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) throws Exception {
        String chartName = genChartByAiRequest.getChartName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(chartName) && chartName.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小
        ThrowUtils.throwIf(size > FILE_SIZE, ErrorCode.PARAMS_ERROR, "文件超过 10M");
        // 校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls", "csv");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        User loginUser = userService.getLoginUser(request);
        // 限流判断，每个用户一个限流器
        redisLimiterServer.doRateLimit("genChartByAi_" + loginUser.getId());

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");

        String csvData = ""; // 在 switch 外部定义

        switch (Objects.requireNonNull(suffix)) {
            case "xlsx":
                csvData = ExcelUtils.XlsxToCsv(multipartFile);
                break;
            case "xls":
                csvData = ExcelUtils.XlsToCsv(multipartFile);
                break;
            case "csv":
                csvData = ExcelUtils.Csv(multipartFile);
                break;
        }
        userInput.append(csvData).append("\n");

        HashMap<String, Object> result = sparkAIServer.sendMesToAItRetry(userInput.toString());
        String chatResult = result.get("chatResult").toString();

        Integer totalTokensInteger = (Integer) result.get("totalTokens");
        long totalTokens = totalTokensInteger.longValue(); // 将 Integer 转换为 Long

        scoreService.deductScore(loginUser.getId(), 1L);
        scoreService.depleteTokens(loginUser.getId(), totalTokens);

        // 匹配{}内的内容
        Pattern pattern = Pattern.compile("\\{(.*)}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(chatResult);
        String genChart;
        String genResult;
        if (matcher.find()) {
            genChart = (matcher.group());
        } else {
            throw new CustomizeException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }

        JsonObject jsonObject = JsonParser.parseString(genChart).getAsJsonObject();
        // 删除标题属性
        jsonObject.remove("title");
        // 将JsonObject转换回字符串
        genChart = jsonObject.toString();

        // 匹配结论后面的内容
        pattern = Pattern.compile("结论：(.*)");
        matcher = pattern.matcher(chatResult);
        if (matcher.find()) {
            genResult = matcher.group(1);
        } else {
            throw new CustomizeException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }
        Chart chart = new Chart();
        // 插入到数据库
        chart.setChartName(chartName);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        chart.setChartStatus("succeed");
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        BiResponseVO biResponseVO = new BiResponseVO();
        biResponseVO.setGenChart(genChart);
        biResponseVO.setGenResult(genResult);
        biResponseVO.setChartId(chart.getId());
        return ResultUtils.success(biResponseVO);
    }

    /**
     * 智能分析（异步）
     *
     * @param multipartFile       文件流
     * @param genChartByAiRequest 图表参数
     * @param request             请求
     * @return 图表的信息
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponseVO> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String chartName = genChartByAiRequest.getChartName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(chartName) && chartName.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小
        ThrowUtils.throwIf(size > FILE_SIZE, ErrorCode.PARAMS_ERROR, "文件超过 10M");
        // 校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls", "csv");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        User loginUser = userService.getLoginUser(request);
        // 限流判断，每个用户一个限流器
        redisLimiterServer.doRateLimit("genChartByAi_" + loginUser.getId());

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");

        String csvData = ""; // 在 switch 外部定义

        switch (Objects.requireNonNull(suffix)) {
            case "xlsx":
                csvData = ExcelUtils.XlsxToCsv(multipartFile);
                break;
            case "xls":
                csvData = ExcelUtils.XlsToCsv(multipartFile);
                break;
            case "csv":
                csvData = ExcelUtils.Csv(multipartFile);
                break;
        }

        userInput.append(csvData).append("\n");

        // 插入到数据库
        Chart chart = new Chart();
        chart.setChartName(chartName);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setChartStatus("wait");
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        CompletableFuture.runAsync(() -> {
            // 先修改图表任务状态为 “执行中”。等执行成功后，修改为 “已完成”、保存执行结果；执行失败后，状态修改为 “失败”，记录任务失败信息。
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setChartStatus("running");
            boolean b = chartService.updateById(updateChart);
            if (!b) {
                handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
                return;
            }

            HashMap<String, Object> result;
            try {
                // 执行重试逻辑
                result = sparkAIServer.sendMesToAItRetry(userInput.toString());
            } catch (Exception e) {
                // 如果重试过程中出现异常，返回错误信息
                throw new CustomizeException(ErrorCode.SYSTEM_ERROR, e + "，AI生成错误");
            }

            String chatResult = result.get("chatResult").toString();
            Integer totalTokensInteger = (Integer) result.get("totalTokens");
            long totalTokens = totalTokensInteger.longValue(); // 将 Integer 转换为 Long

            scoreService.deductScore(loginUser.getId(), 1L);
            scoreService.depleteTokens(loginUser.getId(), totalTokens);

            String userId = String.valueOf(loginUser.getId());
            webSocketServer.sendToClient(userId, "图表生成好啦，快去看看吧！");
            // 匹配{}内的内容
            Pattern pattern = Pattern.compile("\\{(.*)}", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(chatResult);
            String genChart;
            String genResult;
            if (matcher.find()) {
                genChart = (matcher.group());
            } else {
                handleChartUpdateError(chart.getId(), "AI 生成错误");
                return;
            }

            JsonObject jsonObject = JsonParser.parseString(genChart).getAsJsonObject();
            // 删除标题属性
            jsonObject.remove("title");
            // 将JsonObject转换回字符串
            genChart = jsonObject.toString();

            // 匹配结论后面的内容
            pattern = Pattern.compile("结论：(.*)");
            matcher = pattern.matcher(chatResult);
            if (matcher.find()) {
                genResult = matcher.group(1);
            } else {
                handleChartUpdateError(chart.getId(), "AI 生成错误");
                return;
            }
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setChartStatus("succeed");
            boolean updateResult = chartService.updateById(updateChartResult);
            if (!updateResult) {
                handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
            }
        }, threadPoolExecutor);

        BiResponseVO biResponseVO = new BiResponseVO();
        biResponseVO.setChartId(chart.getId());
        return ResultUtils.success(biResponseVO);
    }

    /**
     * 图表更新信息错误拦截器
     *
     * @param chartId     图表id
     * @param execMessage 执行信息
     */
    private void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setChartStatus("failed");
        updateChartResult.setExecMessage(execMessage);
    }


    /**
     * 重新生成图表请求
     *
     * @param chartId 图表id
     * @param request 请求
     * @return
     */
    @GetMapping("/reload/gen")
    public BaseResponse<BiResponseVO> reloadChartByAi(long chartId, HttpServletRequest request) {
        ThrowUtils.throwIf(chartId < 0, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);
        // 限流判断，每个用户一个限流器
        redisLimiterServer.doRateLimit("genChartByAi_" + loginUser.getId());

        Chart byId = chartService.getById(chartId);
        String chartType = byId.getChartType();
        String chartGoal = byId.getGoal();
        String chartData = byId.getChartData();

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = chartGoal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }

        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");

        userInput.append(chartData).append("\n");

        CompletableFuture.runAsync(() -> {
            // 先修改图表任务状态为 “执行中”。等执行成功后，修改为 “已完成”、保存执行结果；执行失败后，状态修改为 “失败”，记录任务失败信息。
            Chart updateChart = new Chart();
            updateChart.setId(chartId);
            updateChart.setChartStatus("running");
            boolean b = chartService.updateById(updateChart);
            if (!b) {
                handleChartUpdateError(chartId, "更新图表执行中状态失败");
                return;
            }

            HashMap<String, Object> result = sparkAIServer.sendMesToAI(userInput.toString());
            String chatResult = result.get("chatResult").toString();
            Integer totalTokensInteger = (Integer) result.get("totalTokens");
            long totalTokens = totalTokensInteger.longValue(); // 将 Integer 转换为 Long

            scoreService.deductScore(loginUser.getId(), 1L);
            scoreService.depleteTokens(loginUser.getId(), totalTokens);

            String userId = String.valueOf(loginUser.getId());
            webSocketServer.sendToClient(userId, "图表生成好啦，快去看看吧！");
            // 匹配{}内的内容
            Pattern pattern = Pattern.compile("\\{(.*)}", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(chatResult);
            String genChart;
            String genResult;
            if (matcher.find()) {
                genChart = (matcher.group());
            } else {
                handleChartUpdateError(chartId, "AI 生成错误");
                return;
            }

            JsonObject jsonObject = JsonParser.parseString(genChart).getAsJsonObject();
            // 删除标题属性
            jsonObject.remove("title");
            // 将JsonObject转换回字符串
            genChart = jsonObject.toString();

            // 匹配结论后面的内容
            pattern = Pattern.compile("结论：(.*)");
            matcher = pattern.matcher(chatResult);
            if (matcher.find()) {
                genResult = matcher.group(1);
            } else {
                handleChartUpdateError(chartId, "AI 生成错误");
                return;
            }
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chartId);
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setChartStatus("succeed");
            boolean updateResult = chartService.updateById(updateChartResult);
            if (!updateResult) {
                handleChartUpdateError(chartId, "更新图表成功状态失败");
            }
        }, threadPoolExecutor);

        BiResponseVO biResponseVO = new BiResponseVO();
        biResponseVO.setChartId(chartId);
        return ResultUtils.success(biResponseVO);
    }


    /**
     * 分页获取图标信息列表（仅管理员）
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        Page<Chart> chartPage = chartService.page(new Page<>(current, size), chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }


    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest 查询参数
     * @return 数据库信息
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String chartName = chartQueryRequest.getChartName();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(chartName), "chart_name", chartName);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chart_type", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "user_id", userId);
        queryWrapper.eq("is_delete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        return queryWrapper;
    }
}
