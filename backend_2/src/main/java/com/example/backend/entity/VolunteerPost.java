package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class VolunteerPost {
  private Long id;
  private String postTitle;
  private String category;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
  private LocalDate deadline;
  private String location;
  private String description;
  private String thumbnail;
  private Integer noOfVolunteer;
  private String orgName;
  private String orgEmail;
}
