package com.example.backend.service.impl;

import com.example.backend.dto.VolunteerRequestDto;
import com.example.backend.entity.Volunteer;
import com.example.backend.entity.VolunteerRequest;
import com.example.backend.messaging.VolunteerRequestMessage;
import com.example.backend.messaging.VolunteerRequestPublisher;
import com.example.backend.repo.VolunteerRequestRepository;
import com.example.backend.service.VolunteerRequestService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import com.example.backend.exception.BadRequestException;
// ResourceNotFoundException not needed after deferring post existence check to consumer
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import com.example.backend.exception.DownstreamServiceException;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static com.example.backend.config.RedisCacheConfig.MY_REQUESTS_BY_EMAIL;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VolunteerRequestServiceImpl implements VolunteerRequestService {

    private final VolunteerRequestRepository requestRepository;
    private final VolunteerRequestPublisher requestPublisher;

    // Async publish only (no direct DB write). Returns -1L to indicate async accepted.
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

        if (postId < 0) {
            throw new BadRequestException("'volunteerPost.id' must be a positive number");
        }
        
        // Defer post existence verification to the consumer to make publish fast.
        // Publish message (contains only postId + volunteer email + suggestion)
        String idempotentKey = currentVolunteer.getVolunteerEmail() + ":" + postId;
        VolunteerRequestMessage msg = new VolunteerRequestMessage(postId,
            currentVolunteer.getVolunteerEmail(),
            suggestion,
            java.time.Instant.now(),
            idempotentKey);
        requestPublisher.publish(msg);

        // Async: cannot return generated DB id now -> use sentinel
        return -1L;
    }

    @SuppressWarnings("unused")
    private Long requestVolunteerFallback(JsonNode body, Volunteer currentVolunteer, Throwable ex) {
        throw new DownstreamServiceException("Failed to enqueue volunteer request", ex);
    }

    @Override
    @Retry(name = "volunteerRequestService")
    @CircuitBreaker(name = "volunteerRequestService", fallbackMethod = "getMyVolunteerRequestsFallback")
    @Bulkhead(name = "volunteerRequestService", type = Bulkhead.Type.SEMAPHORE)
    @Cacheable(
        cacheNames = MY_REQUESTS_BY_EMAIL,
        key = "#email.toLowerCase() + ':' + (#postId != null ? #postId : 'all') + ':p:' + #pageable.pageNumber",
        unless = "#result == null || #result.isEmpty()"
    )
    public Page<VolunteerRequestDto> getMyVolunteerRequests(String email, Long postId, Pageable pageable) {
        if (email == null || email.isBlank()) {
            return Page.empty(pageable != null ? pageable : PageRequest.of(0, 1));
        }

        Pageable effective = pageable != null ? pageable : PageRequest.of(0, 10);
        Page<VolunteerRequest> requests = (postId != null)
            ? requestRepository.findByVolunteerVolunteerEmailAndVolunteerPostId(email, postId, effective)
            : requestRepository.findByVolunteerVolunteerEmail(email, effective);

        return requests.map(this::toDto);
    }

    @Deprecated
    public List<VolunteerRequestDto> getMyVolunteerRequests(String email) {
        return requestRepository.findByVolunteerVolunteerEmail(email).stream()
            .map(this::toDto)
            .toList();
    }

    @SuppressWarnings("unused")
    private Page<VolunteerRequestDto> getMyVolunteerRequestsFallback(String email, Long postId, Pageable pageable, Throwable ex) {
        throw new DownstreamServiceException("Failed to load volunteer requests for " + email, ex);
    }

    private VolunteerRequestDto toDto(VolunteerRequest request) {
        var dto = new VolunteerRequestDto();
        var post = request.getVolunteerPost();
        var volunteer = request.getVolunteer();
        dto.setId(request.getId());
        dto.setPostId(post != null ? post.getId() : null);
        dto.setStatus(request.getStatus());
        dto.setVolunteerEmail(volunteer != null ? volunteer.getVolunteerEmail() : null);
        if (post != null) {
            dto.setPostTitle(post.getPostTitle());
            dto.setOrgEmail(post.getOrgEmail());
            dto.setDeadline(post.getDeadline() != null ? post.getDeadline().toString() : null);
            dto.setLocation(post.getLocation());
            dto.setCategory(post.getCategory());
        }
        return dto;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = MY_REQUESTS_BY_EMAIL, allEntries = true) // cache eviction will also be handled in consumer
    public void removeVolunteerRequest(Long id) {
        if (id == null || id <= 0) {
            return;
        }
        // Publish delete message; validation and deletion happen in consumer
        com.example.backend.messaging.DeleteVolunteerRequestMessage msg =
                new com.example.backend.messaging.DeleteVolunteerRequestMessage(id, java.time.Instant.now());
        requestPublisher.publishDelete(msg);
    }
}
