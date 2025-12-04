package com.example.backend.messaging;

import com.example.backend.config.RedisCacheConfig;
import com.example.backend.entity.Volunteer;
import com.example.backend.entity.VolunteerPost;
import com.example.backend.entity.VolunteerRequest;
import com.example.backend.repo.VolunteerPostRepository;
import com.example.backend.repo.VolunteerRepository;
import com.example.backend.repo.VolunteerRequestRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class VolunteerRequestConsumer {

    private final VolunteerPostRepository postRepository;
    private final VolunteerRepository volunteerRepository;
    private final VolunteerRequestRepository requestRepository;
    private final CacheManager cacheManager;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @RabbitListener(queues = RabbitConfig.QUEUE)
    @Transactional
    public void handleVolunteerRequest(VolunteerRequestMessage message) {
        if (message == null || message.getPostId() == null || message.getVolunteerEmail() == null) {
            return; // drop invalid
        }
        // Use primitive id to match custom repository method; fall back to Optional if needed
        Long postId = message.getPostId();
        VolunteerPost post = (postId != null) ? postRepository.findById(postId.longValue()) : null;
        if (post == null) {
            // Post not found: ACK and drop message (business decision)
            logger.warn("Dropping volunteer request message: post not found (postId={})", postId);
            return;
        }

        // Require an existing Volunteer record in DB for the provided email; if missing, drop message
        var volunteerOpt = volunteerRepository.findByVolunteerEmail(message.getVolunteerEmail());
        if (volunteerOpt.isEmpty()) {
            logger.warn("Dropping volunteer request message: volunteer not found (email={})", message.getVolunteerEmail());
            return;
        }
        Volunteer volunteer = volunteerOpt.get();

        // Rely on DB uniqueness to enforce single request per user/post (idempotent by DB)

        VolunteerRequest request = new VolunteerRequest();
        request.setVolunteerPost(post);
        request.setVolunteer(volunteer);
        request.setSuggestion(message.getSuggestion());
        request.setStatus("Pending");
        request.setRequestDate(LocalDateTime.now());

        // Idempotent create: rely on DB unique constraint (volunteerEmail, postId)
        try {
            requestRepository.save(request);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Duplicate insert prevented by DB uniqueness -> drop silently
            logger.info("Duplicate volunteer request prevented by DB (key={}): {}", message.getIdempotentKey(), ex.getMessage());
            return;
        }

        // Evict cache of this volunteer's requests so next read is fresh
        if (cacheManager != null) {
            var cache = cacheManager.getCache(RedisCacheConfig.MY_REQUESTS_BY_EMAIL);
            if (cache != null) {
                cache.evict(volunteer.getVolunteerEmail().toLowerCase());
            }
        }
    }

    @RabbitListener(queues = RabbitConfig.DELETE_QUEUE)
    @Transactional
    public void handleDeleteVolunteerRequest(DeleteVolunteerRequestMessage message) {
        if (message == null || message.getRequestId() == null) {
            return;
        }
        Long requestId = message.getRequestId();
        var reqOpt = requestRepository.findById(requestId);
        if (reqOpt.isEmpty()) {
            logger.info("Skipping delete: request not found (id={})", requestId);
            return;
        }
        VolunteerRequest req = reqOpt.get();
        // If the request was previously accepted, decrement the post's volunteer count
        try {
            var post = req.getVolunteerPost();
            if (req.getStatus() != null && req.getStatus().equalsIgnoreCase("Accepted") && post != null && post.getId() != null) {
                int updated = postRepository.decrementVolunteerCount(post.getId());
                if (updated > 0) {
                    logger.info("Decremented volunteer count for post id={}", post.getId());
                } else {
                    logger.info("Volunteer count not decremented (already zero) for post id={}", post.getId());
                }
            }
        } catch (Exception e) {
            // Log but continue to attempt deletion of request
            logger.warn("Failed to decrement volunteer count for request id={}: {}", requestId, e.getMessage());
        }

        requestRepository.delete(req);

        // Evict cache for this volunteer's requests
        if (cacheManager != null && req.getVolunteer() != null) {
            var cache = cacheManager.getCache(RedisCacheConfig.MY_REQUESTS_BY_EMAIL);
            if (cache != null) {
                cache.evict(req.getVolunteer().getVolunteerEmail().toLowerCase());
            }
        }
    }
}
