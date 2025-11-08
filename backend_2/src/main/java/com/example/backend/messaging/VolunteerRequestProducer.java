package com.example.backend.messaging;

import com.example.backend.config.RabbitMQConfig;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VolunteerRequestProducer {

  @Autowired
  private RabbitTemplate rabbitTemplate;

  public String enqueueCreateRequest(String volunteerEmail, Long postId, String suggestion) {
    String requestId = UUID.randomUUID().toString();
    CreateVolunteerRequestCommand cmd = new CreateVolunteerRequestCommand(
            requestId,
            volunteerEmail,
            postId,
            suggestion,
            System.currentTimeMillis()
    );

    rabbitTemplate.convertAndSend(
            RabbitMQConfig.REQUEST_EXCHANGE,
            RabbitMQConfig.REQUEST_ROUTING_KEY,
            cmd,
            message -> {
              message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
              return message;
            }
    );

    return requestId;
  }
}
