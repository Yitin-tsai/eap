package com.eap.eap_matchengine.configuration.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  @Bean
  public Queue orderCreatedQueue() {
    return new Queue("order.created.queue");
  }

  @Bean
  public Queue orderMatchedQueue() {
    return new Queue("order.matched.queue");
  }

  @Bean
  public Binding binding(Queue orderCreatedQueue, TopicExchange orderExchange) {
    return BindingBuilder.bind(orderCreatedQueue).to(orderExchange).with("order.created");
  }

  @Bean
  public Binding matchedBinding(Queue orderMatchedQueue, TopicExchange orderExchange) {
    return BindingBuilder.bind(orderMatchedQueue).to(orderExchange).with("order.matched");
  }
}
