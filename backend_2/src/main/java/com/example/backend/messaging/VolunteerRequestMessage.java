package com.example.backend.messaging;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class VolunteerRequestMessage {
    private String trackingId;
    private Long postId;
    private String volunteerEmail;
    private String suggestion;
    private LocalDateTime requestDate;
}