package com.example.backend.messaging;

import com.example.backend.entity.Volunteer;
import com.example.backend.entity.VolunteerPost;
import com.example.backend.entity.VolunteerRequest;
import com.example.backend.repo.VolunteerPostRepository;
import com.example.backend.repo.VolunteerRepository;
import com.example.backend.repo.VolunteerRequestRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class VolunteerRequestConsumer {

  @Autowired
  private VolunteerPostRepository postRepository;

  @Autowired
  private VolunteerRepository volunteerRepository;

  @Autowired
  private VolunteerRequestRepository requestRepository;

  @RabbitListener(queues = "volunteer.request.queue", concurrency = "2-8")
  @Transactional
  public void handle(CreateVolunteerRequestCommand cmd) {
    // Load entities
    Volunteer volunteer = volunteerRepository.findByVolunteerEmail(cmd.getVolunteerEmail())
            .orElseGet(() -> {
              // Create if not exists to avoid FK issue
              Volunteer v = new Volunteer();
              v.setVolunteerEmail(cmd.getVolunteerEmail());
              return volunteerRepository.save(v);
            });

    VolunteerPost post = postRepository.findById(cmd.getPostId())
            .orElse(null);
    if (post == null) {
      // No post -> skip (could route to DLQ by throwing)
      throw new IllegalArgumentException("Post not found: " + cmd.getPostId());
    }

    // Idempotency is enforced by unique constraint (volunteerEmail, postId)
    VolunteerRequest vr = new VolunteerRequest();
    vr.setVolunteer(volunteer);
    vr.setVolunteerPost(post);
    vr.setSuggestion(cmd.getSuggestion());
    vr.setStatus("Pending");
    vr.setRequestDate(LocalDateTime.now());

    try {
      requestRepository.save(vr);
    } catch (org.springframework.dao.DataIntegrityViolationException ex) {
      // Duplicate (unique constraint), treat as idempotent success
    }
  }
}
