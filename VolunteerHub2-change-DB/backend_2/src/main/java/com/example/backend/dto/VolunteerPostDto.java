package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.io.Serializable;

public class VolunteerPostDto implements Serializable {

  private static final long serialVersionUID = 1L;
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

  public VolunteerPostDto() {
  }

  public VolunteerPostDto(Long id,
      String postTitle,
      String category,
      LocalDate deadline,
      String location,
      String description,
      String thumbnail,
      Integer noOfVolunteer,
      String orgName,
      String orgEmail) {
    this.id = id;
    this.postTitle = postTitle;
    this.category = category;
    this.deadline = deadline;
    this.location = location;
    this.description = description;
    this.thumbnail = thumbnail;
    this.noOfVolunteer = noOfVolunteer;
    this.orgName = orgName;
    this.orgEmail = orgEmail;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPostTitle() {
    return postTitle;
  }

  public void setPostTitle(String postTitle) {
    this.postTitle = postTitle;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public LocalDate getDeadline() {
    return deadline;
  }

  public void setDeadline(LocalDate deadline) {
    this.deadline = deadline;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getThumbnail() {
    return thumbnail;
  }

  public void setThumbnail(String thumbnail) {
    this.thumbnail = thumbnail;
  }

  public Integer getNoOfVolunteer() {
    return noOfVolunteer;
  }

  public void setNoOfVolunteer(Integer noOfVolunteer) {
    this.noOfVolunteer = noOfVolunteer;
  }

  public String getOrgName() {
    return orgName;
  }

  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }

  public String getOrgEmail() {
    return orgEmail;
  }

  public void setOrgEmail(String orgEmail) {
    this.orgEmail = orgEmail;
  }
}
