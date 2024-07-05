package com.zzy.project.bizmq;

import com.rabbitmq.client.Channel;
import com.zzy.project.common.ErrorCode;
import com.zzy.project.constant.BiMqConstant;
import com.zzy.project.constant.StatusConstant;
import com.zzy.project.exception.BusinessException;
import com.zzy.project.model.entity.Chart;
import com.zzy.project.service.ChartService;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BiMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private ChartService chartService;

    /**
     * 向消息队列中发送消息
     *
     * @param message 发送的消息
     */
    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(BiMqConstant.BI_EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY, message);
    }

}