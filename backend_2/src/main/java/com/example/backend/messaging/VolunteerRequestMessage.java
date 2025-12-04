package com.example.backend.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VolunteerRequestMessage implements Serializable {
    private Long postId;
    private String volunteerEmail;
    private String suggestion;
    private Instant createdAt;
    // Idempotent key format: "userId:eventId" -> here "volunteerEmail:postId"
    private String idempotentKey;
}
