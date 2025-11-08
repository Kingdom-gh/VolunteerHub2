package com.example.backend.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVolunteerRequestCommand {
  private String requestId;
  private String volunteerEmail;
  private Long postId;
  private String suggestion;
  private long createdAtEpochMillis;
}
