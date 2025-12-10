package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;
import java.io.Serializable;

@Entity
@Table(name = "volunteer_post")
@Data
public class VolunteerPost implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id; // Tương đương id

//  @JsonProperty("postTitle")
  @Column(name = "postTitle")
  private String postTitle;

  private String category;

  @Column(name = "deadline")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
  private LocalDate deadline; // Sử dụng LocalDate cho deadline

  @Column(name = "location")
  private String location;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "thumbnail")
  private String thumbnail;

//  @JsonProperty("noOfVolunteer")
  @Column(name = "noOfVolunteer")
  private Integer noOfVolunteer; // Số lượng tình nguyện viên cần

//  @JsonProperty("orgName")
  @Column(name = "orgName")
  private String orgName;

//  @JsonProperty("orgEmail")
  @Column(name = "orgEmail")
  private String orgEmail;

  @OneToMany(mappedBy = "volunteerPost", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<VolunteerRequest> requests;
}
