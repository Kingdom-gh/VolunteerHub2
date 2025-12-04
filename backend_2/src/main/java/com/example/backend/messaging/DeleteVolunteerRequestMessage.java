package com.example.backend.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteVolunteerRequestMessage implements Serializable {
    private Long requestId;
    private Instant createdAt;
}
