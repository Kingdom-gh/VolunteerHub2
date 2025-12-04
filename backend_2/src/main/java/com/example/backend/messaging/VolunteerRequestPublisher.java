package com.example.backend.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VolunteerRequestPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(VolunteerRequestMessage message) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, message);
    }

    public void publishDelete(DeleteVolunteerRequestMessage message) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.DELETE_ROUTING_KEY, message);
    }
}
