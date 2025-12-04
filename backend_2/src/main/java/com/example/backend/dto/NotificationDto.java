package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String title;
    private String body;
    private String dataJson;
    private String link;
    private LocalDateTime createdAt;
}
