package com.example.backend.repo;


import com.example.backend.entity.VolunteerRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VolunteerRequestRepository extends JpaRepository<VolunteerRequest, Long> {

  // API: get-volunteer-request/:email (Lấy các request đã đăng ký)
  List<VolunteerRequest> findByVolunteerVolunteerEmail(String volunteerEmail);

  // API: get-volunteer-requests-for-org/:email (Lấy các request gửi tới các post của organizer)
  List<VolunteerRequest> findByVolunteerPostOrgEmail(String orgEmail);

  // Paged requests for a specific post
  org.springframework.data.domain.Page<VolunteerRequest> findByVolunteerPostId(Long postId, org.springframework.data.domain.Pageable pageable);

  // Count pending requests for a specific post
  long countByVolunteerPostIdAndStatusIgnoreCase(Long postId, String status);
  // (Removed) uniqueness enforced by DB unique constraint; consumer handles DataIntegrityViolationException
}