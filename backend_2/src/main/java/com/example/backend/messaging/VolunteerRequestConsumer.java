package com.example.backend.messaging;

import com.example.backend.config.RedisCacheConfig;
import com.example.backend.entity.Volunteer;
import com.example.backend.entity.VolunteerPost;
import com.example.backend.entity.VolunteerRequest;
import com.example.backend.repo.VolunteerPostRepository;
import com.example.backend.repo.VolunteerRepository;
import com.example.backend.repo.VolunteerRequestRepository;
import lombok.RequiredArgsConstructor;
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
            return; // invalid post, ignore
        }
        Volunteer volunteer = volunteerRepository.findByVolunteerEmail(message.getVolunteerEmail())
                .orElseGet(() -> {
                    Volunteer v = new Volunteer();
                    v.setVolunteerEmail(message.getVolunteerEmail());
                    return volunteerRepository.save(v);
                });

        VolunteerRequest request = new VolunteerRequest();
        request.setVolunteerPost(post);
        request.setVolunteer(volunteer);
        request.setSuggestion(message.getSuggestion());
        request.setStatus("Pending");
        request.setRequestDate(LocalDateTime.now());

        requestRepository.save(request);

        // Evict cache of this volunteer's requests so next read is fresh
        if (cacheManager != null) {
            var cache = cacheManager.getCache(RedisCacheConfig.MY_REQUESTS_BY_EMAIL);
            if (cache != null) {
                cache.evict(volunteer.getVolunteerEmail().toLowerCase());
            }
        }
    }
}
