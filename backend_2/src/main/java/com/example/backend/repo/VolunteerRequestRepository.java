package com.example.backend.repo;


import com.example.backend.entity.VolunteerRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface VolunteerRequestRepository extends JpaRepository<VolunteerRequest, Long> {

  // API: get-volunteer-request/:email (Lấy các request đã đăng ký)
  List<VolunteerRequest> findByVolunteerVolunteerEmail(String volunteerEmail);

  Page<VolunteerRequest> findByVolunteerVolunteerEmail(String volunteerEmail, Pageable pageable);

  Page<VolunteerRequest> findByVolunteerVolunteerEmailAndVolunteerPostId(String volunteerEmail, Long postId, Pageable pageable);

  // API: get-volunteer-requests-for-org/:email (Lấy các request gửi tới các post của organizer)
  List<VolunteerRequest> findByVolunteerPostOrgEmail(String orgEmail);

  Page<VolunteerRequest> findByVolunteerPostOrgEmail(String orgEmail, Pageable pageable);

  // Paged requests for a specific post
  org.springframework.data.domain.Page<VolunteerRequest> findByVolunteerPostId(Long postId, org.springframework.data.domain.Pageable pageable);

  // Count pending requests for a specific post
  long countByVolunteerPostIdAndStatusIgnoreCase(Long postId, String status);
  // (Removed) uniqueness enforced by DB unique constraint; consumer handles DataIntegrityViolationException

  @Query("SELECT vr.volunteerPost.id AS postId, COUNT(vr) AS pendingCount "
      + "FROM VolunteerRequest vr "
      + "WHERE LOWER(vr.status) = 'pending' "
      + "AND LOWER(vr.volunteerPost.orgEmail) = LOWER(:orgEmail) "
      + "AND vr.volunteerPost.id IN :postIds "
      + "GROUP BY vr.volunteerPost.id")
  List<PendingCountView> countPendingForPosts(@Param("orgEmail") String orgEmail,
                                              @Param("postIds") List<Long> postIds);

  interface PendingCountView {
    Long getPostId();
    long getPendingCount();
  }
}