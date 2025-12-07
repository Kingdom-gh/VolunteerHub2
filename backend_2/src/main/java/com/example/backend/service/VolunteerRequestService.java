package com.example.backend.service;

import com.example.backend.dto.VolunteerRequestDto;
import com.example.backend.entity.Volunteer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VolunteerRequestService {
    // POST /request-volunteer  (body: JsonNode, user: @AuthenticationPrincipal)
    Long requestVolunteer(JsonNode body, Volunteer currentVolunteer);

    // GET /get-volunteer-request/{email}
    Page<VolunteerRequestDto> getMyVolunteerRequests(String email, Long postId, Pageable pageable);

    List<VolunteerRequestDto> getMyVolunteerRequests(String email);

    // DELETE /my-volunteer-request/{id}
    void removeVolunteerRequest(Long id);
}