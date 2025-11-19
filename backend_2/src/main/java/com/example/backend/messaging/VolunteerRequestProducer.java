package com.example.backend.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VolunteerRequestProducer {
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;
    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    public void send(VolunteerRequestMessage message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}