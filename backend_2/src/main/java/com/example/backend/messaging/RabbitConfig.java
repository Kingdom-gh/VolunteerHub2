package com.example.backend.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "volunteer.request.exchange";
    public static final String ROUTING_KEY = "volunteer.request.created";
    public static final String QUEUE = "volunteer.request.queue";

    @Bean
    public TopicExchange volunteerRequestExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue volunteerRequestQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding volunteerRequestBinding(Queue volunteerRequestQueue, TopicExchange volunteerRequestExchange) {
        return BindingBuilder.bind(volunteerRequestQueue).to(volunteerRequestExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
