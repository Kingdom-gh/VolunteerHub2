package com.example.backend.service.impl;

import com.example.backend.dto.VolunteerRequestDto;
import com.example.backend.entity.Volunteer;
import com.example.backend.entity.VolunteerPost;
import com.example.backend.entity.VolunteerRequest;
import com.example.backend.messaging.VolunteerRequestMessage;
import com.example.backend.messaging.VolunteerRequestPublisher;
import com.example.backend.repo.VolunteerPostRepository;
import com.example.backend.repo.VolunteerRequestRepository;
import com.example.backend.service.VolunteerRequestService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.ResourceNotFoundException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import com.example.backend.exception.DownstreamServiceException;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.backend.config.RedisCacheConfig.MY_REQUESTS_BY_EMAIL;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VolunteerRequestServiceImpl implements VolunteerRequestService {

    private final VolunteerRequestRepository requestRepository;
    private final VolunteerPostRepository postRepository;
    private final VolunteerRequestPublisher requestPublisher;

    // Async publish only (no direct DB write). Returns -1L to indicate async accepted.
    @CircuitBreaker(name = "volunteerRequestService", fallbackMethod = "requestVolunteerFallback")
    @Bulkhead(name = "volunteerRequestService", type = Bulkhead.Type.SEMAPHORE)
    public Long requestVolunteer(JsonNode body, Volunteer currentVolunteer) {
        if (currentVolunteer == null) {
            throw new IllegalStateException("Unauthorized: missing volunteer principal");
        }

        var postIdNode = body.path("volunteerPost").path("id");
        var suggestionNode = body.get("suggestion");
        if (postIdNode.isMissingNode() || !postIdNode.canConvertToInt() || suggestionNode == null) {
            throw new BadRequestException("Missing or invalid 'volunteerPost.id' or 'suggestion'");
        }

        long postId = postIdNode.asLong();
        String suggestion = suggestionNode.asText();

        // Optional validation to reject immediately if post not found (avoid queue spam)
        VolunteerPost post = postRepository.findById(postId);
        if (post == null) {
            throw new ResourceNotFoundException("Post not found: " + postId);
        }

        // Publish message
        VolunteerRequestMessage msg = new VolunteerRequestMessage(postId,
                currentVolunteer.getVolunteerEmail(),
                suggestion,
                java.time.Instant.now());
        requestPublisher.publish(msg);

        // Async: cannot return generated DB id now -> use sentinel
        return -1L;
    }

    @SuppressWarnings("unused")
    private Long requestVolunteerFallback(JsonNode body, Volunteer currentVolunteer, Throwable ex) {
        throw new DownstreamServiceException("Failed to enqueue volunteer request", ex);
    }

    @Retry(name = "volunteerRequestService")
    @CircuitBreaker(name = "volunteerRequestService", fallbackMethod = "getMyVolunteerRequestsFallback")
    @Bulkhead(name = "volunteerRequestService", type = Bulkhead.Type.SEMAPHORE)
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
            if (post != null) {
                dto.setPostTitle(post.getPostTitle());
                dto.setOrgEmail(post.getOrgEmail());
                dto.setDeadline(post.getDeadline() != null ? post.getDeadline().toString() : null);
                dto.setLocation(post.getLocation());
                dto.setCategory(post.getCategory());
            }
            return dto;
        }).toList();
    }

    @SuppressWarnings("unused")
    private List<VolunteerRequestDto> getMyVolunteerRequestsFallback(String email, Throwable ex) {
        throw new DownstreamServiceException("Failed to load volunteer requests for " + email, ex);
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
