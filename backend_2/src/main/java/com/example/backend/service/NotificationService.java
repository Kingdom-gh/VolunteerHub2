package com.example.backend.service;

import com.example.backend.dto.NotificationDto;
import com.example.backend.entity.Notification;
import com.example.backend.repo.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;
    private final SimpMessagingTemplate messaging;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createAndSend(String recipientEmail, String title, String body, Map<String, Object> data, String link) {
        try {
            Notification n = new Notification();
            n.setRecipientEmail(recipientEmail.toLowerCase());
            n.setTitle(title);
            n.setBody(body);
            if (data != null) {
                n.setDataJson(objectMapper.writeValueAsString(data));
            }
            n.setLink(link);
            repo.save(n);

            NotificationDto dto = new NotificationDto(n.getId(), n.getTitle(), n.getBody(), n.getDataJson(), n.getLink(), n.getCreatedAt());
            // send to topic specific to recipient (client should subscribe to /topic/notifications.{email})
            try {
                messaging.convertAndSend("/topic/notifications." + recipientEmail.toLowerCase(), dto);
            } catch (Exception e) {
                logger.warn("Failed to push notification to topic for {}: {}", recipientEmail, e.getMessage());
            }
            return n;
        } catch (Exception ex) {
            logger.error("Failed to create/send notification: {}", ex.getMessage());
            return null;
        }
    }

    public Page<Notification> getNotifications(String recipientEmail, Pageable pageable) {
        return repo.findByRecipientEmailOrderByCreatedAtDesc(recipientEmail.toLowerCase(), pageable);
    }

    public long countUnread(String recipientEmail) {
        return repo.countByRecipientEmailAndReadFlagFalse(recipientEmail.toLowerCase());
    }
}
