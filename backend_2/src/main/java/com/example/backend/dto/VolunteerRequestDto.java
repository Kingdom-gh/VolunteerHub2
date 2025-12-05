package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;


@Data
public class VolunteerRequestDto implements Serializable {
  private static final long serialVersionUID = 1L;
  private Long id;              // ID của VolunteerRequest (cho hành động Hủy)
  private Long postId;          // ID của VolunteerPost
  private String postTitle;     // From VolunteerPost
  private String orgEmail;      // Từ VolunteerPost
  private String deadline;      // Từ VolunteerPost
  private String location;      // Từ VolunteerPost
  private String category;      // Từ VolunteerPost
  private String status; // Từ VolunteerRequest
   // Từ Volunteer (để gửi về client)
}
