package com.zzy.project.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.zzy.project.constant.BiMqConstant;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class BiInitMain {
    public static void main(String[] args)  {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        Connection connection = null;
        try {
            connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(BiMqConstant.BI_EXCHANGE_NAME,"direct");
            //创建队列，并分配交换机
            // 声明队列：
            // 参数1：queueName，队列名称
            // 参数2：durable，是否持久化队列
            //        true：队列在服务器重启后仍然存在
            // 参数3：exclusive，是否排他队列
            //        false：队列可以被多个连接共享
            // 参数4：autoDelete，是否自动删除
            //        false：服务器不再使用该队列时不自动删除
            // 参数5：arguments，队列的其他属性（如：TTL、死信交换机等）
            channel.queueDeclare(BiMqConstant.BI_QUEUE_NAME,true,false,false,null);
            // 绑定队列到交换机：
            // 参数1：queueName，队列名称
            // 参数2：exchangeName，交换机名称
            // 参数3：routingKey，路由键，用于将消息路由到队列
            channel.queueBind(BiMqConstant.BI_QUEUE_NAME,BiMqConstant.BI_EXCHANGE_NAME,BiMqConstant.BI_ROUTING_KEY);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
