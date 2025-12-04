package com.example.backend.service;

import com.example.backend.dto.VolunteerRequestDto;
import com.example.backend.entity.Volunteer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public interface VolunteerRequestService {
    // POST /request-volunteer  (body: JsonNode, user: @AuthenticationPrincipal)
    Long requestVolunteer(JsonNode body, Volunteer currentVolunteer);

    // GET /get-volunteer-request/{email}
    List<VolunteerRequestDto> getMyVolunteerRequests(String email);

    // DELETE /my-volunteer-request/{id}
    void removeVolunteerRequest(Long id);
}