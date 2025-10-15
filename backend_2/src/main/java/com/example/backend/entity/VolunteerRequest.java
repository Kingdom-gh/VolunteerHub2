package com.example.backend.entity;


import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "volunteer_requests") // Bảng mới cho Request
@Data
public class VolunteerRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Thông tin người đăng ký
  @Column(name = "volunteerEmail")
  private String volunteerEmail;

  @Column(name = "volunteerName")
  private String volunteerName;

  // Thông tin về bài post
  @Column(name = "postId")
  private Long postId; // Foreign key tới VolunteerPost

  @Column(name = "postTitle")
  private String postTitle;

  @Column(name = "orgEmail")
  private String orgEmail;

  @Column(name = "requestDate")
  private LocalDateTime requestDate = LocalDateTime.now(); // Thời gian request

  private String status = "Pending"; // Trạng thái của request (Pending, Approved, Rejected)
}