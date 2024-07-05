package com.zzy.project.bizmq;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.zzy.project.constant.BiMqConstant.*;

/**
 * 声明一个队列并绑定交换机
 */
@Configuration
public class RabbitMQConfig{
    @Bean
    ApplicationRunner runner(ConnectionFactory cf) {
        return args -> cf.createConnection().close();
    }
    /**
     * 创建队列并且这指定死性队列
     * @return
     */
    @Bean
    public Queue biQueue(){
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange",BI_DLQ_NAME);
        args.put("x-dead-letter-routing-key", BI_DLQ_ROUTING_KEY);
        return new Queue(BI_QUEUE_NAME,true,false,false,args);
    }
    @Bean
    public DirectExchange biExchange() {
        return new DirectExchange(BI_EXCHANGE_NAME);
    }

    @Bean
    public Binding BiBinding(Queue biQueue, DirectExchange biExchange) {
        return BindingBuilder.bind(biQueue).to(biExchange).with(BI_ROUTING_KEY);
    }

    /**
     * 声明死刑队列
     * @return
     */
    @Bean
    public Queue deadLetterQueue(){
        return new Queue(BI_DLQ_NAME,true);
    }
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(BI_DLQ_EXCHANGE_NAME);
    }
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(BI_DLQ_ROUTING_KEY);
    }
}  
