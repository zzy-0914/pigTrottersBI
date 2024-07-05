package com.zzy.project.ScheduledJob;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zzy.project.constant.StatusConstant;
import com.zzy.project.mapper.ChartMapper;
import com.zzy.project.model.entity.Chart;
import com.zzy.project.service.ChartService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Component
@Slf4j
public class RenTaskScheduler {
    @Resource
    ChartService chartService;
    @Resource
    private ChartMapper chartMapper;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Scheduled(cron = "0 0 0 * * ?")  // 每天0点执行定时任务
    public void processFailedTasks() {
        //1.首先从数据库中找出状态为失败的队列
        QueryWrapper<Chart> chartQueryWrapper = new QueryWrapper<>();
        chartQueryWrapper.eq("status", StatusConstant.FAILED);
        List<Chart> charts = chartMapper.selectList(chartQueryWrapper);
        //2.更新失败的图表
        charts.forEach((chart)->{
            CompletableFuture.runAsync(() -> {
                chartService.updateChartResult(chart.getId(),getUserSaying(chart));
            }, threadPoolExecutor);
        });
    }
    public String getUserSaying(Chart chart){
        //构造用户输入
        StringBuilder userSay = new StringBuilder();
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();
        String userGoal = goal;
        userSay.append("需求分析：").append("\n");
        if (StringUtils.isNotBlank(chartType)){
            userGoal = userGoal+",请使用类型"+chartType;
        }
        userSay.append(userGoal).append("\n");
        userSay.append("原始数据：").append("\n");
        userSay.append(csvData).append("\n");
        return userSay.toString();
    }
}