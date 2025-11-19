package com.example.backend.messaging;

import com.example.backend.entity.Volunteer;
import com.example.backend.entity.VolunteerPost;
import com.example.backend.entity.VolunteerRequest;
import com.example.backend.repo.VolunteerPostRepository;
import com.example.backend.repo.VolunteerRepository;
import com.example.backend.repo.VolunteerRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.example.backend.config.RedisCacheConfig.MY_REQUESTS_BY_EMAIL;

@Component
@RequiredArgsConstructor
@Slf4j
public class VolunteerRequestConsumer {

    private final VolunteerRequestRepository requestRepository;
    private final VolunteerPostRepository postRepository;
    private final VolunteerRepository volunteerRepository;

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    @CacheEvict(cacheNames = MY_REQUESTS_BY_EMAIL, key = "#message.volunteerEmail.toLowerCase()")
    public void handleVolunteerRequest(VolunteerRequestMessage message) {
        try {
            long postId = message.getPostId();
            VolunteerPost post = postRepository.findById(postId); // dùng overload trả về entity trực tiếp
            if (post == null) {
                log.warn("[VolunteerRequestConsumer] Post not found id={} trackingId={}", message.getPostId(), message.getTrackingId());
                return; // bỏ qua: post không tồn tại
            }
            var volunteerOpt = volunteerRepository.findByVolunteerEmail(message.getVolunteerEmail());
            if (volunteerOpt.isEmpty()) {
                log.warn("[VolunteerRequestConsumer] Volunteer not found email={} trackingId={}", message.getVolunteerEmail(), message.getTrackingId());
                return; // bỏ qua: volunteer không tồn tại
            }
            Volunteer volunteer = volunteerOpt.get();

            VolunteerRequest entity = new VolunteerRequest();
            entity.setVolunteerPost(post);
            entity.setVolunteer(volunteer);
            entity.setSuggestion(message.getSuggestion());
            entity.setStatus("Pending");
            entity.setRequestDate(message.getRequestDate() != null ? message.getRequestDate() : LocalDateTime.now());

            requestRepository.save(entity);
            log.info("[VolunteerRequestConsumer] Persisted volunteer request trackingId={} postId={} volunteerEmail={}",
                    message.getTrackingId(), message.getPostId(), message.getVolunteerEmail());
        } catch (Exception ex) {
            log.error("[VolunteerRequestConsumer] Error processing trackingId={} : {}", message.getTrackingId(), ex.getMessage(), ex);
            // TODO: có thể gửi vào dead-letter queue hoặc retry sau
        }
    }
}