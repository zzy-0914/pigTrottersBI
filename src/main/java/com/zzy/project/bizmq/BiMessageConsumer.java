package com.zzy.project.bizmq;

import com.rabbitmq.client.Channel;
import com.zzy.project.common.ErrorCode;
import com.zzy.project.constant.BiMqConstant;
import com.zzy.project.constant.StatusConstant;
import com.zzy.project.exception.BusinessException;
import com.zzy.project.manager.AiManager;
import com.zzy.project.model.entity.Chart;
import com.zzy.project.service.ChartService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.sql.Statement;

@Component
@Slf4j
public class BiMessageConsumer {

    @Resource
    private ChartService chartService;
    //指定程序监听的消息和确认机制
    @SneakyThrows
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME},ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receiveMessage message = {}", message);
        //1.检验消息的正确性
        if (StringUtils.isBlank(message)){
            //消息为空，消息拒绝，直接丢弃
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
        }
        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if (chart==null){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表为空");
        }
        //2.先修改图表任务状态为 “执行中”。等执行成功后，修改为 “已完成”、保存执行结果；执行失败后，状态修改为 “失败”，记录任务失败信息。
        String saying = buildUserInput(chart);
        try {
            boolean b = chartService.updateChartResult(chartId, saying);
            if (!b){
                throw  new BusinessException(ErrorCode.SYSTEM_ERROR,"更新异常");
            }
            channel.basicNack(deliveryTag,true,true);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, false);
            log.error("消息处理失败，消息放入死信队列: {}", message, e);
        }
    }
    @RabbitListener(queues = {BiMqConstant.BI_DLQ_NAME},ackMode = "MANUAL")
    public void receiveErrorMessage(String message,Channel channel,@Header(AmqpHeaders.DELIVERY_TAG) long deliverTag){
        if (StringUtils.isBlank(message)){
            throwExceptionAndNackMessage(channel,deliverTag);
        }
        log.info("receiveErrorMessage message = {}", message);
        Chart chart = chartService.getById(message);
        if (chart==null){
            throwExceptionAndNackMessage(channel,deliverTag);
        }
        Chart updateChart = new Chart();
        updateChart.setId(Long.parseLong(message));
        updateChart.setStatus(StatusConstant.FAILED);
        chartService.updateById(updateChart);
        try {
            channel.basicAck(deliverTag, false);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    private void throwExceptionAndNackMessage(Channel channel, long deliveryTag) {
        try {
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR);
    }
    /**
     * 构建用户输入
     * @param chart
     * @return
     */
    private String buildUserInput(Chart chart) {
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

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
        userInput.append(csvData).append("\n");
        return userInput.toString();
    }
}
