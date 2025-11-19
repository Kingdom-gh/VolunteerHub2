package com.example.backend.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;
    @Value("${app.rabbitmq.queue}")
    private String queueName;
    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    @Bean
    public DirectExchange volunteerRequestExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue volunteerRequestQueue() {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding volunteerRequestBinding(Queue volunteerRequestQueue, DirectExchange volunteerRequestExchange) {
        return BindingBuilder.bind(volunteerRequestQueue).to(volunteerRequestExchange).with(routingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}