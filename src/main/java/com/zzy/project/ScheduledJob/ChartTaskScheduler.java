package com.zzy.project.ScheduledJob;

import cn.hutool.core.util.ObjectUtil;
import com.github.rholder.retry.Retryer;
import com.zzy.project.controller.ChartController;
import com.zzy.project.manager.AiManager;
import com.zzy.project.manager.RedisLimiterManager;
import com.zzy.project.model.dto.chart.ScheduledChart;
import com.zzy.project.service.ChartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Component
@Slf4j
public class ChartTaskScheduler {
    @Resource
    ChartService chartService;
    private AiManager aiManager;
    @Resource
    private Retryer<Boolean> retryer;

    @Resource
    private RedisLimiterManager redisLimiterManager;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Scheduled(fixedRate = 50000)  // 每5秒执行一次
    public void processFailedTasks() {
        //首先获取需要失败状态的图表
        List<ScheduledChart> chartList = ChartController.chartList;
        if (ObjectUtil.isEmpty(chartList)){
            return;
        }
        chartList.stream().forEach((scheduledChart)->{
            Callable<Boolean> callable = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try {
                        CompletableFuture.runAsync(() -> {
                            chartService.updateChartResult(scheduledChart.getChartId(),scheduledChart.getUserSay());
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
                    //添加成功，将失败状态的图表 从对列中取出
                    chartList.remove(scheduledChart);
                } catch (Exception e) {
                    log.error("失败状态的图表:"+scheduledChart.getChartId()+" 更新失败",e);
            }
        });


    }

}
