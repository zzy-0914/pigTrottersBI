package com.zzy.project.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.Scanner;

public class DlxDirectProducer {
    private static final String EXCHANGE_NAME = "dlx-exchanger";
    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            //声明死信交换机
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
            // 创建队列，随机分配一个队列名称
            String queueName = "waibao_queue";
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, EXCHANGE_NAME, "xiaoyu");

            // 创建队列，随机分配一个队列名称
            String queueName2 = "laoban_queue";
            channel.queueDeclare(queueName2, true, false, false, null);
            channel.queueBind(queueName2, EXCHANGE_NAME, "xiaopi");

            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                String userInput = scanner.nextLine();
                String[] strings = userInput.split(" ");
                if (strings.length < 1) {
                    continue;
                }
                String message = strings[0];
                String routingKey = strings[1];

                channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
                System.out.println(" [x] Sent '" + message + " with routing:" + routingKey + "'");
            }

        }
    }
}
