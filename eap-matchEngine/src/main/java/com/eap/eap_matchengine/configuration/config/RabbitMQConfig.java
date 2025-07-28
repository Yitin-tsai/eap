package com.eap.eap_matchengine.configuration.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.eap.common.constants.RabbitMQConstants.*;

/**
 * RabbitMQ 配置類
 * 負責定義交易系統所需的所有 Queue、Exchange 和 Binding
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 訂單創建隊列
     */
    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE);
    }

    /**
     * 訂單匹配隊列
     */
    @Bean
    public Queue orderMatchedQueue() {
        return new Queue(ORDER_MATCHED_QUEUE);
    }

    /**
     * 錢包資產處理隊列
     */
    @Bean
    public Queue walletMatchedQueue() {
        return new Queue(WALLET_MATCHED_QUEUE);
    }

    /**
     * 訂單交換機
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }


    @Bean
    public Queue orderCancelQueue() {
        return new Queue(ODER_CANCEL_QUEUE);
    }

    /**
     * 訂單創建消息綁定
     */
    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderCreatedQueue)
                .to(orderExchange)
                .with(ORDER_CREATED_KEY);
    }

    /**
     * 訂單匹配消息綁定
     */
    @Bean
    public Binding orderMatchedBinding(Queue orderMatchedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderMatchedQueue)
                .to(orderExchange)
                .with(ORDER_MATCHED_KEY);
    }

    /**
     * 錢包資產處理消息綁定
     */
    @Bean
    public Binding walletMatchedBinding(Queue walletMatchedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(walletMatchedQueue)
                .to(orderExchange)
                .with(WALLET_MATCHED_KEY);
    }

    @Bean
    public Binding orderCancelBinding(Queue orderCancelQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderCancelQueue)
                .to(orderExchange)
                .with(ORDER_CANCEL_KEY);
    }
    /**
     * 配置消息轉換器，使用 JSON 格式
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
