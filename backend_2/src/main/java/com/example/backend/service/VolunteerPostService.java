package com.example.backend.service;

import com.example.backend.dto.VolunteerPostDto;
import com.example.backend.entity.VolunteerPost;

import java.util.List;

public interface VolunteerPostService {
    // GET /volunteers (6 bài gần hết hạn, dùng cache)
    List<VolunteerPostDto> getLatestVolunteers();

    // GET /need-volunteers?search=  (dùng cache)
    List<VolunteerPostDto> getAllVolunteers(String search);

    // GET /post/{id} (dùng cache)
    VolunteerPostDto getVolunteerPostDetails(Long id);

    // POST /add-volunteer-post (ghi dữ liệu, không cần DTO)
    Long addVolunteerPost(VolunteerPost post);

    // PUT /update-volunteer-count/{id}  (giảm số lượng)
    int decrementVolunteerCount(Long id);

    // PUT /update-volunteer-post/{id}
    void updateVolunteerPost(Long id, VolunteerPost updatedData);

    // DELETE /my-volunteer-post/{id}
    void deleteVolunteerPost(Long id);

    // GET /get-volunteer-post/{email} (dùng cache)
    List<VolunteerPostDto> getMyVolunteerPosts(String email);
}
