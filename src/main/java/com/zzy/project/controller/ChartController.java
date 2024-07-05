package com.zzy.project.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.rholder.retry.Retryer;
import com.zzy.project.annotation.AuthCheck;
import com.zzy.project.bizmq.BiMessageProducer;
import com.zzy.project.common.BaseResponse;
import com.zzy.project.common.DeleteRequest;
import com.zzy.project.common.ErrorCode;
import com.zzy.project.common.ResultUtils;
import com.zzy.project.constant.CommonConstant;
import com.zzy.project.exception.BusinessException;
import com.zzy.project.exception.ThrowUtils;
import com.zzy.project.manager.AiManager;
import com.zzy.project.manager.RedisLimiterManager;
import com.zzy.project.model.dto.chart.*;
import com.zzy.project.model.entity.Chart;
import com.zzy.project.model.entity.User;
import com.zzy.project.model.vo.BiResponse;
import com.zzy.project.service.ChartService;
import com.zzy.project.service.UserService;
import com.zzy.project.utils.ExcelUtils;
import com.zzy.project.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.*;

import static com.zzy.project.constant.StatusConstant.*;

/**
 *
 * @author zzy
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;
    @Resource
    private Retryer<Boolean> retryer;

    @Resource
    private RedisLimiterManager redisLimiterManager;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;
    // region 增删改查
    public static int count =0;
    public static List<ScheduledChart> chartList = new CopyOnWriteArrayList<>();
    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest,HttpServletRequest request){
        //1.首先先判断是否传过来的是空数据
        if (chartAddRequest==null){
            throw  new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest,chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        //2.将传过来的数据存入数据库
        boolean result = chartService.save(chart);
        if (!result){
            throw  new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(chart.getId());
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        if (oldChart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新
     *
     * @param chartUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest,
                                             HttpServletRequest request) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        // 参数校验

        User user = userService.getLoginUser(request);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        if (oldChart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可修改
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 获取列表（仅管理员可使用）
     *
     * @param chartQueryRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list")
    public BaseResponse<List<Chart>> listChart(ChartQueryRequest chartQueryRequest) {
        Chart chartQuery = new Chart();
        if (chartQueryRequest != null) {
            BeanUtils.copyProperties(chartQueryRequest, chartQuery);
        }
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>(chartQuery);
        List<Chart> chartList = chartService.list(queryWrapper);
        return ResultUtils.success(chartList);
    }


    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }
    /**
     * 智能分析（同步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String chartType = genChartByAiRequest.getChartType();
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        ThrowUtils.throwIf(StringUtils.isAnyBlank(name,goal,chartType)||name.length()>100,
                ErrorCode.PARAMS_ERROR);
        //用户输入信息
        User loginUser = userService.getLoginUser(request);
        //指定对应的AI模型
        long biModelId =1798633473824202753L;
        //文件校验
        long file_size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        //1.文件大小
        final long FILE_MAX_SIZE =1024*1024L;
        ThrowUtils.throwIf(file_size>FILE_MAX_SIZE,ErrorCode.PARAMS_ERROR,"文件过大");
        //指定后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> FILE_SUFFIX = Arrays.asList("xlsx","xls");
        ThrowUtils.throwIf(!FILE_SUFFIX.contains(suffix),ErrorCode.PARAMS_ERROR,"文件格式错误");
        //限流判断
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());
        //构造用户输入
        StringBuilder userSay = new StringBuilder();
        String userGoal = goal;
        userSay.append("需求分析：").append("\n");
        if (StringUtils.isNotBlank(chartType)){
            userGoal = userGoal+",请使用类型"+chartType;
        }
        userSay.append(userGoal).append("\n");
        userSay.append("原始数据：").append("\n");
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userSay.append(csvData).append("\n");
        //获取返回结果
        System.out.println(userSay.toString());
        String result = aiManager.doChat(biModelId, userSay.toString());
        String[] splits = result.split("【【【【【");
        // 拆分之后还要进行校验
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 生成错误");
        }

        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        // 插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        chartService.createTable(chart.getId(), multipartFile);
        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析（异步，线程池）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request)  {
        String chartType = genChartByAiRequest.getChartType();
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        ThrowUtils.throwIf(StringUtils.isAnyBlank(name,goal,chartType)||name.length()>100,
                ErrorCode.PARAMS_ERROR);
        //用户输入信息
        User loginUser = userService.getLoginUser(request);
        //指定对应的AI模型

        //文件校验
        long file_size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        //1.文件大小
        final long FILE_MAX_SIZE =1024*1024L;
        ThrowUtils.throwIf(file_size>FILE_MAX_SIZE,ErrorCode.PARAMS_ERROR,"文件过大");
        //指定后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> FILE_SUFFIX = Arrays.asList("xlsx","xls");
        ThrowUtils.throwIf(!FILE_SUFFIX.contains(suffix),ErrorCode.PARAMS_ERROR,"文件格式错误");
        //限流判断
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());
        //构造用户输入
        StringBuilder userSay = new StringBuilder();
        String userGoal = goal;
        userSay.append("需求分析：").append("\n");
        if (StringUtils.isNotBlank(chartType)){
            userGoal = userGoal+",请使用类型"+chartType;
        }
        userSay.append(userGoal).append("\n");
        userSay.append("原始数据：").append("\n");
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userSay.append(csvData).append("\n");
        // 用户点击智能分析页的提交按钮时，先把图表立刻保存到数据库中，然后提交任务
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        // 任务状态字段(排队中wait、执行中running、已完成succeed、失败failed),封住成枚举
        //设置任务在排队状态
        chart.setStatus(WAIT);
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        chartService.createTable(chart.getId(), multipartFile);
        // 在最终的返回结果前提交一个任务
        // todo 建议处理任务队列满了后,抛异常的情况(因为提交任务报错了,前端会返回异常)
        Callable<Boolean> callable = new Callable<Boolean>() {
            @Override
            public Boolean call()  {
                try {
                    count++;
                    CompletableFuture.runAsync(() -> {
                        boolean result = chartService.updateChartResult(chart.getId(), userSay.toString());
                        if (!result){
                            throw  new BusinessException(ErrorCode.SYSTEM_ERROR,"更新异常");
                        }
                    }, threadPoolExecutor);
                } catch (Exception e) {
                    return false;
                }
                return true;
            }
        };
        Boolean call = null;
        try {
            call = retryer.call(callable);
        } catch (Exception e) {
            //将chartId 和 需要查询的数据 封装起来，交给定时任务处理
            ScheduledChart scheduledChart = new ScheduledChart();
            scheduledChart.setChartId(chart.getId());
            scheduledChart.setUserSay(userSay.toString());
            chartList.add(scheduledChart);
        }
        BiResponse biResponse = new BiResponse();
        // biResponse.setGenChart(genChart);
        // biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析（消息队列）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request)  {
        String chartType = genChartByAiRequest.getChartType();
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        ThrowUtils.throwIf(StringUtils.isAnyBlank(name,goal,chartType)||name.length()>100,
                ErrorCode.PARAMS_ERROR);
        //用户输入信息
        User loginUser = userService.getLoginUser(request);
        //指定对应的AI模型

        //文件校验
        long file_size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        //1.文件大小
        final long FILE_MAX_SIZE =1024*1024L;
        ThrowUtils.throwIf(file_size>FILE_MAX_SIZE,ErrorCode.PARAMS_ERROR,"文件过大");
        //指定后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> FILE_SUFFIX = Arrays.asList("xlsx","xls");
        ThrowUtils.throwIf(!FILE_SUFFIX.contains(suffix),ErrorCode.PARAMS_ERROR,"文件格式错误");
        //限流判断
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());
        //构造用户输入
        StringBuilder userSay = new StringBuilder();
        String userGoal = goal;
        userSay.append("需求分析：").append("\n");
        if (StringUtils.isNotBlank(chartType)){
            userGoal = userGoal+",请使用类型"+chartType;
        }
        userSay.append(userGoal).append("\n");
        userSay.append("原始数据：").append("\n");
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userSay.append(csvData).append("\n");
        // 用户点击智能分析页的提交按钮时，先把图表立刻保存到数据库中，然后提交任务
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        // 任务状态字段(排队中wait、执行中running、已完成succeed、失败failed),封住成枚举
        //设置任务在排队状态
        chart.setStatus(WAIT);
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        //分库分表
        chartService.createTable(chart.getId(), multipartFile);
        // 在最终的返回结果前提交一个任务
        biMessageProducer.sendMessage(String.valueOf(chart.getId()));

        BiResponse biResponse = new BiResponse();
        // biResponse.setGenChart(genChart);
        // biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }
    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}
