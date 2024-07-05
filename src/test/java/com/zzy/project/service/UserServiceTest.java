package com.zzy.project.service;

import com.zzy.project.model.entity.Chart;
import com.zzy.project.model.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static com.zzy.project.constant.StatusConstant.RUNNING;
import static com.zzy.project.constant.StatusConstant.SUCCEED;

/**
 * 用户服务测试
 *
 * @author yupi
 */
@SpringBootTest
class UserServiceTest {

    @Resource
    private UserService userService;

    @Test
    void testAddUser() {
        User user = new User();
        boolean result = userService.save(user);
        System.out.println(user.getId());
        Assertions.assertTrue(result);
    }

    @Test
    void testUpdateUser() {
        User user = new User();
        boolean result = userService.updateById(user);
        Assertions.assertTrue(result);
    }

    @Test
    void testDeleteUser() {
        boolean result = userService.removeById(1L);
        Assertions.assertTrue(result);
    }

    @Test
    void testGetUser() {
        User user = userService.getById(1L);
        Assertions.assertNotNull(user);
    }

    @Test
    void userRegister() {
        // Callable<Boolean> callable = new Callable<Boolean>() {
        //     @Override
        //     public Boolean call() throws Exception {
        //         try {
        //             CompletableFuture.runAsync(() -> {
        //                 Chart updateChart = new Chart();
        //                 updateChart.setId(chart.getId());
        //                 //将运行状态改为 running
        //                 updateChart.setStatus(RUNNING);
        //                 boolean b = chartService.updateById(updateChart);
        //                 if (!b) {
        //                     handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
        //                     return;
        //                 }
        //                 //进行AI查询
        //                 String result = aiManager.doChat(biModelId, userSay.toString());
        //                 String[] splits = result.split("【【【【【");
        //                 // 拆分之后还要进行校验
        //                 if (splits.length < 3) {
        //                     handleChartUpdateError(chart.getId(), "AI 生成错误");
        //                     return;
        //                 }
        //                 String genChart = splits[1].trim();
        //                 String genResult = splits[2].trim();
        //                 //生成成功，更新数据库
        //                 Chart updateChartResult = new Chart();
        //                 updateChartResult.setId(chart.getId());
        //                 updateChartResult.setGenChart(genChart);
        //                 updateChartResult.setGenResult(genResult);
        //                 updateChartResult.setStatus(SUCCEED);
        //                 boolean updateResult = chartService.updateById(updateChartResult);
        //                 if (!updateResult) {
        //                     handleChartUpdateError(chart.getId(), "更新图表失败");
        //                 }
        //             }, threadPoolExecutor);
        //         } catch (Exception e) {
        //             System.out.println();
        //             return false;
        //         }
        //         return true;
        //     }
        // };
        // Boolean call = retryer.call(callable);
    }

}