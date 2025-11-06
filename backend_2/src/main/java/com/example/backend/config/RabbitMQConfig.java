package com.example.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

  public static final String REQUEST_EXCHANGE = "volunteer.request.exchange";
  public static final String REQUEST_QUEUE = "volunteer.request.queue";
  public static final String REQUEST_ROUTING_KEY = "volunteer.request.create";

  public static final String DLX = "volunteer.request.dlx";
  public static final String DLQ = "volunteer.request.dlq";
  public static final String DLQ_ROUTING_KEY = "volunteer.request.dlq";

  @Bean
  public DirectExchange requestExchange() {
    return new DirectExchange(REQUEST_EXCHANGE, true, false);
  }

  @Bean
  public DirectExchange deadLetterExchange() {
    return new DirectExchange(DLX, true, false);
  }

  @Bean
  public Queue requestQueue() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-dead-letter-exchange", DLX);
    args.put("x-dead-letter-routing-key", DLQ_ROUTING_KEY);
    return new Queue(REQUEST_QUEUE, true, false, false, args);
  }

  @Bean
  public Queue deadLetterQueue() {
    return new Queue(DLQ, true);
  }

  @Bean
  public Binding requestBinding() {
    return BindingBuilder.bind(requestQueue()).to(requestExchange()).with(REQUEST_ROUTING_KEY);
  }

  @Bean
  public Binding dlqBinding() {
    return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ_ROUTING_KEY);
  }

  @Bean
  public Jackson2JsonMessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(messageConverter());
    template.setMandatory(true);
    return template;
  }

  // Optional: customize connection factory if needed
  // Using Spring Boot auto-configured ConnectionFactory
}
