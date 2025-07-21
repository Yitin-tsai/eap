package com.eap.eap_wallet.configuration.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static com.eap.common.constants.RabbitMQConstants.*;

@Configuration
public class RabbitMQConfig {

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public TopicExchange orderExchange() {
    return new TopicExchange(ORDER_EXCHANGE);
  }

  @Bean
  public Queue orderCreateQueue() {
    return new Queue(ORDER_CREATE_QUEUE);
  }

  @Bean
  public Queue orderCreatedQueue() {
    return new Queue(ORDER_CREATED_QUEUE);
  }

  @Bean
  public Queue orderMatchedQueue() {
    return new Queue(ORDER_MATCHED_QUEUE);
  }

  @Bean
  public Binding orderCreateBinding(@Qualifier("orderCreateQueue") Queue orderCreateQueue,
      TopicExchange orderExchange) {
    return BindingBuilder.bind(orderCreateQueue).to(orderExchange).with(ORDER_CREATE_KEY);
  }

  @Bean
  public Binding binding(@Qualifier("orderCreatedQueue") Queue orderCreatedQueue, TopicExchange orderExchange) {
    return BindingBuilder.bind(orderCreatedQueue).to(orderExchange).with(ORDER_CREATED_KEY);
  }

  @Bean
  public Binding matchedBinding(@Qualifier("orderMatchedQueue") Queue orderMatchedQueue, TopicExchange orderExchange) {
    return BindingBuilder.bind(orderMatchedQueue).to(orderExchange).with(ORDER_MATCHED_KEY);
  }
}
