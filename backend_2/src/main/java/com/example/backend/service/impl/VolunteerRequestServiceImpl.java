package com.example.backend.service.impl;

import com.example.backend.dto.VolunteerRequestDto;
import com.example.backend.entity.Volunteer;
import com.example.backend.entity.VolunteerPost;
import com.example.backend.entity.VolunteerRequest;
import com.example.backend.repo.VolunteerPostRepository;
import com.example.backend.repo.VolunteerRequestRepository;
import com.example.backend.service.VolunteerRequestService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.backend.config.RedisCacheConfig.MY_REQUESTS_BY_EMAIL;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VolunteerRequestServiceImpl implements VolunteerRequestService {

    private final VolunteerRequestRepository requestRepository;
    private final VolunteerPostRepository postRepository;

    @Override
    @Transactional
    @CacheEvict(
        cacheNames = MY_REQUESTS_BY_EMAIL,
        key = "#currentVolunteer.volunteerEmail.toLowerCase()",
        condition = "#currentVolunteer != null && #currentVolunteer.volunteerEmail != null"
    )
    public Long requestVolunteer(JsonNode body, Volunteer currentVolunteer) {
        if (currentVolunteer == null) {
            throw new IllegalStateException("Unauthorized: missing volunteer principal");
        }

        var postIdNode = body.path("volunteerPost").path("id");
        var suggestionNode = body.get("suggestion");
        if (postIdNode.isMissingNode() || !postIdNode.canConvertToInt() || suggestionNode == null) {
            throw new IllegalArgumentException("Missing or invalid 'volunteerPost.id' or 'suggestion'");
        }

        long postId = postIdNode.asLong();
        String suggestion = suggestionNode.asText();

        // ✅ findById trả Optional -> dùng orElseThrow cho rõ ràng
        // Ở dự án này findById trả trực tiếp VolunteerPost (có thể null)
        VolunteerPost post = postRepository.findById(postId);
        if (post == null) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }


        VolunteerRequest req = new VolunteerRequest();
        req.setVolunteerPost(post);
        req.setVolunteer(currentVolunteer); // Controller đã lấy principal; nếu cần entity managed, đảm bảo đã load từ DB
        req.setSuggestion(suggestion);
        req.setStatus("Pending");
        req.setRequestDate(LocalDateTime.now()); // đã import

        return requestRepository.saveAndFlush(req).getId();
    }

    @Override
    @Cacheable(
        cacheNames = MY_REQUESTS_BY_EMAIL,
        key = "#email.toLowerCase()",
        unless = "#result == null || #result.isEmpty()"
    )
    public List<VolunteerRequestDto> getMyVolunteerRequests(String email) {
        var requests = requestRepository.findByVolunteerVolunteerEmail(email);
        return requests.stream().map(request -> {
            var post = request.getVolunteerPost();
            var dto = new VolunteerRequestDto();
            dto.setId(request.getId());
            dto.setStatus(request.getStatus());
            if (post != null) { // ✅ tránh NPE nếu post bị null
                dto.setPostTitle(post.getPostTitle());
                dto.setOrgEmail(post.getOrgEmail());
                dto.setDeadline(post.getDeadline() != null ? post.getDeadline().toString() : null);
                dto.setLocation(post.getLocation());
                dto.setCategory(post.getCategory());
            }
            return dto;
        }).toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = MY_REQUESTS_BY_EMAIL, allEntries = true) // đơn giản
    public void removeVolunteerRequest(Long id) {
        if (!requestRepository.existsById(id)) {
            return; // không ném lỗi; Controller có thể trả 404 trước khi gọi service
        }
        requestRepository.deleteById(id);
    }
}
