package com.example.backend.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VolunteerRequest {
  private Long id;

  @JsonIgnore
  private VolunteerPost volunteerPost;

  @JsonIgnore
  private Volunteer volunteer;

  private LocalDateTime requestDate = LocalDateTime.now();
  private String suggestion;
  private String status = "Pending";
}