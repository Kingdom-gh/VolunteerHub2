package com.example.backend.controller;

import com.example.backend.dto.VolunteerRequestDto;
import com.example.backend.entity.Volunteer;
import com.example.backend.entity.VolunteerPost;
import com.example.backend.repo.VolunteerRepository;
import com.example.backend.security.JwtService;
import com.example.backend.service.VolunteerPostService;
import com.example.backend.service.VolunteerRequestService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.example.backend.repo.VolunteerRequestRepository;
import com.example.backend.dto.VolunteerPostDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.backend.service.NotificationService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = {"http://localhost:5173", "https://volunteer-management-sys-66dad.web.app"}, allowCredentials = "true")
@Transactional
@RequiredArgsConstructor
public class VolunteerController {

  // Giữ lại VolunteerRepository cho 2 endpoint JWT (thêm user nếu chưa có)
  private final VolunteerRepository volunteerRepository;

  // JWT service giữ nguyên
  private final JwtService jwtService;

  // ✅ Thêm Service layer
  private final VolunteerPostService postService;
  private final VolunteerRequestService requestService;

  private final VolunteerRequestRepository requestRepository;
  private final NotificationService notificationService;

  // --- JWT/AUTH ENDPOINTS ---
  @PostMapping("/jwt")
  public ResponseEntity<?> createJwt(@RequestBody Map<String, String> user, HttpServletResponse response) {
    String email = user.get("email");
    if (email == null) {
      return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
    }

    // Thêm user vào DB nếu chưa có (giữ nguyên logic cũ)
    Optional<Volunteer> existingVolunteer = volunteerRepository.findByVolunteerEmail(email);
    if (existingVolunteer.isEmpty()) {
      Volunteer newVolunteer = new Volunteer();
      newVolunteer.setVolunteerEmail(email);
      volunteerRepository.save(newVolunteer);
    }

    // Tạo JWT
    String token = jwtService.generateToken(email);
    // Set-Cookie: token=...; HttpOnly; Path=/; SameSite=None
    response.addHeader("Set-Cookie", jwtService.createCookieHeader("token", token, true));

    return ResponseEntity.ok(Map.of("success", true));
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpServletResponse response) {
    // Xoá cookie
    response.addHeader("Set-Cookie", jwtService.createCookieHeader("token", "", false, 0));
    return ResponseEntity.ok(Map.of("success", true));
  }

  // --- VOLUNTEER POSTS ENDPOINTS ---

  // Home Page - 6 bài gần hết hạn
  @GetMapping("/volunteers")
  public List<VolunteerPostDto> getLatestVolunteers() {
    return postService.getLatestVolunteers();
  }

  // Tất cả bài, có tìm kiếm
  @GetMapping("/need-volunteers")
  public Page<VolunteerPostDto> getAllVolunteers(
      @RequestParam(required = false) String search,
      @PageableDefault(size = 15) Pageable pageable) {
    // Bỏ qua tham số size từ client, luôn cố định size=15
    Pageable effective = PageRequest.of(pageable.getPageNumber(), 15);
    return postService.getAllVolunteers(search, effective);
  }

  // Chi tiết bài
  @GetMapping("/post/{id}")
  public ResponseEntity<VolunteerPostDto> getVolunteerPostDetails(@PathVariable Long id) {
    VolunteerPostDto post = postService.getVolunteerPostDetails(id);
    return (post != null) ? ResponseEntity.ok(post) : ResponseEntity.notFound().build();
  }

@PostMapping("/add-volunteer-post")
public ResponseEntity<?> addVolunteerPost(@RequestBody VolunteerPost post,
                                          @AuthenticationPrincipal Volunteer current) {
  post.setId(null); // ép insert

  if (current != null) {
    String email = (current.getVolunteerEmail() != null && !current.getVolunteerEmail().isBlank())
        ? current.getVolunteerEmail()
        : current.getUsername();
    if (email == null || email.isBlank()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("message", "Login required"));
    }
    post.setOrgEmail(email); // lấy từ principal
  } else {
    // ✅ Nếu không login, buộc body phải có orgEmail
    if (post.getOrgEmail() == null || post.getOrgEmail().isBlank()) {
      return ResponseEntity.badRequest()
          .body(Map.of("message", "orgEmail is required (login or include in body)"));
    }
  }

  Long insertedId = postService.addVolunteerPost(post);
  return ResponseEntity.ok(Map.of("insertedId", insertedId));
}

  // Giảm số lượng
  @PutMapping("/update-volunteer-count/{id}")
  public ResponseEntity<?> updateVolunteerCount(@PathVariable Long id) {
    int updatedRows = postService.decrementVolunteerCount(id);
    if (updatedRows == 0) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "Post not found or volunteer count already zero"));
    }
    return ResponseEntity.ok(Map.of("success", true, "updatedRows", updatedRows));
  }

  // Cập nhật bài
  @PutMapping("/update-volunteer-post/{id}")
  public ResponseEntity<?> updateVolunteerPost(@PathVariable Long id,
      @RequestBody VolunteerPost updatedData) {
    // ĐỔI VolunteerPost -> VolunteerPostDto
    com.example.backend.dto.VolunteerPostDto existing = postService.getVolunteerPostDetails(id);
    if (existing == null) {
      return ResponseEntity.notFound().build();
    }
    postService.updateVolunteerPost(id, updatedData);
    return ResponseEntity.ok(Map.of("success", true));
  }

  // Xoá bài của tôi
  @DeleteMapping("/my-volunteer-post/{id}")
  public ResponseEntity<?> deleteVolunteerPost(@PathVariable Long id) {
    // ĐỔI VolunteerPost -> VolunteerPostDto
    VolunteerPostDto existing = postService.getVolunteerPostDetails(id);
    if (existing == null) {
      return ResponseEntity.notFound().build();
    }
    postService.deleteVolunteerPost(id);
    return ResponseEntity.ok(Map.of("success", true));
  }

  // Lấy danh sách bài theo org email
  @GetMapping("/get-volunteer-post/{email}")
  public List<VolunteerPostDto> getMyVolunteerPosts(@PathVariable String email) {
    return postService.getMyVolunteerPosts(email);
  }

  // --- VOLUNTEER REQUESTS ENDPOINTS ---

  // Gửi yêu cầu tham gia

@PostMapping("/request-volunteer")
public ResponseEntity<?> requestVolunteer(@RequestBody JsonNode body,
                                          @AuthenticationPrincipal Volunteer currentVolunteer) {
  if (currentVolunteer == null) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body("User must be authenticated to submit a request.");
  }

  // Use the authentication principal only (do not synchronously look up user in DB here)
  // Consumer will decide whether the volunteer record exists or not.
  String email = currentVolunteer.getVolunteerEmail();
  if (email == null || email.isBlank()) {
    email = currentVolunteer.getUsername();
  }

  Long insertedId = requestService.requestVolunteer(body, currentVolunteer);
  return ResponseEntity.ok(Map.of("insertedId", insertedId));
}


  // Lấy yêu cầu của tôi
  @GetMapping("/get-volunteer-request/{email}")
  public List<VolunteerRequestDto> getMyVolunteerRequests(@PathVariable String email) {
    return requestService.getMyVolunteerRequests(email);
  }

  // Lấy yêu cầu gửi tới các post của organizer (theo orgEmail)
  @GetMapping("/get-volunteer-requests-for-org/{email}")
  public List<VolunteerRequestDto> getRequestsForOrganizer(@PathVariable String email,
                                                           @AuthenticationPrincipal Volunteer current) {
    // Optional auth reinforce: require principal to match orgEmail
    String principalEmail = (current != null && current.getVolunteerEmail() != null && !current.getVolunteerEmail().isBlank())
        ? current.getVolunteerEmail() : (current != null ? current.getUsername() : null);
    if (principalEmail == null || !principalEmail.equalsIgnoreCase(email)) {
      return List.of();
    }
    var requests = requestRepository.findByVolunteerPostOrgEmail(email);
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

  // Pending count per post for organizer
  @GetMapping("/org/{email}/posts-with-pending-count")
  public List<Map<String, Object>> getPostsWithPendingCounts(@PathVariable String email,
                                                             @AuthenticationPrincipal Volunteer current) {
    String principalEmail = (current != null && current.getVolunteerEmail() != null && !current.getVolunteerEmail().isBlank())
        ? current.getVolunteerEmail() : (current != null ? current.getUsername() : null);
    if (principalEmail == null || !principalEmail.equalsIgnoreCase(email)) {
      return List.of();
    }
    var posts = postService.getMyVolunteerPosts(email);
    return posts.stream().map(p -> Map.<String, Object>of(
      "id", p.getId(),
      "postTitle", p.getPostTitle(),
      "pendingCount", requestRepository.countByVolunteerPostIdAndStatusIgnoreCase(p.getId(), "Pending")
    )).toList();
  }

  // Paged requests for a specific post (fixed page size = 10)
  @GetMapping("/post/{postId}/requests")
  public Page<VolunteerRequestDto> getRequestsForPost(@PathVariable Long postId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @AuthenticationPrincipal Volunteer current) {
    // Auth: ensure principal owns the post
    var postDto = postService.getVolunteerPostDetails(postId);
    String principalEmail = (current != null && current.getVolunteerEmail() != null && !current.getVolunteerEmail().isBlank())
        ? current.getVolunteerEmail() : (current != null ? current.getUsername() : null);
    if (postDto == null || principalEmail == null || !principalEmail.equalsIgnoreCase(postDto.getOrgEmail())) {
      return Page.empty();
    }
    // Fixed page size to 10 regardless of client input
    final int FIXED_PAGE_SIZE = 10;
    Pageable pageable = PageRequest.of(page, FIXED_PAGE_SIZE);
    var pageReq = requestRepository.findByVolunteerPostId(postId, pageable);
    return pageReq.map(request -> {
      var dto = new VolunteerRequestDto();
      dto.setId(request.getId());
      dto.setStatus(request.getStatus());
      var post = request.getVolunteerPost();
      if (post != null) {
        dto.setPostTitle(post.getPostTitle());
        dto.setOrgEmail(post.getOrgEmail());
        dto.setDeadline(post.getDeadline() != null ? post.getDeadline().toString() : null);
        dto.setLocation(post.getLocation());
        dto.setCategory(post.getCategory());
      }
      return dto;
    });
  }

  // Huỷ yêu cầu
  @DeleteMapping("/my-volunteer-request/{id}")
  public ResponseEntity<?> removeVolunteerRequest(@PathVariable Long id) {
    // 1) Kiểm tra tồn tại trực tiếp bằng repository (nhanh & chính xác)
    if (!requestRepository.existsById(id)) {
      return ResponseEntity.notFound().build();
    }

    // 2) Gọi service xoá (service hiện là void và đã @CacheEvict)
    requestService.removeVolunteerRequest(id);

    // 3) Trả kết quả OK
    return ResponseEntity.ok(Map.of("success", true));
  }

  // --- APPROVAL FLOW ---
  @PutMapping("/volunteer-request/{id}/approve")
  @Transactional
  public ResponseEntity<?> approveVolunteerRequest(@PathVariable Long id,
                                                   @AuthenticationPrincipal Volunteer current) {
    var opt = requestRepository.findById(id);
    if (opt.isEmpty()) return ResponseEntity.notFound().build();
    var req = opt.get();

    var post = req.getVolunteerPost();
    if (post == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("message", "Missing post for request"));

    // Permission: only post owner can approve
    String principalEmail = (current != null && current.getVolunteerEmail() != null && !current.getVolunteerEmail().isBlank())
        ? current.getVolunteerEmail() : (current != null ? current.getUsername() : null);
    if (principalEmail == null || !principalEmail.equalsIgnoreCase(post.getOrgEmail())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("message", "Only post owner can approve"));
    }

    // Guard: already decided
    String status = req.getStatus();
    if (status != null && (status.equalsIgnoreCase("Accepted") || status.equalsIgnoreCase("Rejected"))) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "Request is already decided"));
    }

    // Guard: available slots
    Integer slots = post.getNoOfVolunteer();
    if (slots != null && slots <= 0) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "No available slots"));
    }

    // Update status then decrement count atomically
    req.setStatus("Accepted");
    requestRepository.save(req);
    int updated = postService.decrementVolunteerCount(post.getId());
    if (updated == 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Failed to decrement count");
    }

    // Create notifications (best-effort)
    try {
      String volunteerEmail = req.getVolunteer() != null ? req.getVolunteer().getVolunteerEmail() : null;
      String postTitle = post.getPostTitle() != null ? post.getPostTitle() : "(unknown)";
      if (volunteerEmail != null) {
        notificationService.createAndSend(volunteerEmail,
            "Request Accepted",
            "Your request for post: " + postTitle + " has been accepted.",
            Map.of("postId", post.getId(), "requestId", req.getId()),
            "/posts/" + post.getId());
      }
      // notify organiser who approved
      String orgEmail = post.getOrgEmail();
      if (orgEmail != null) {
        notificationService.createAndSend(orgEmail,
            "Request Approved",
            "You have approved a request for post: " + postTitle + ".",
            Map.of("postId", post.getId(), "requestId", req.getId()),
            "/manage/posts/" + post.getId());
      }
    } catch (Exception ignored) {}

    return ResponseEntity.ok(Map.of("success", true));
  }

  @PutMapping("/volunteer-request/{id}/reject")
  @Transactional
  public ResponseEntity<?> rejectVolunteerRequest(@PathVariable Long id,
                                                  @AuthenticationPrincipal Volunteer current) {
    var opt = requestRepository.findById(id);
    if (opt.isEmpty()) return ResponseEntity.notFound().build();
    var req = opt.get();

    var post = req.getVolunteerPost();
    if (post == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("message", "Missing post for request"));

    String principalEmail = (current != null && current.getVolunteerEmail() != null && !current.getVolunteerEmail().isBlank())
        ? current.getVolunteerEmail() : (current != null ? current.getUsername() : null);
    if (principalEmail == null || !principalEmail.equalsIgnoreCase(post.getOrgEmail())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("message", "Only post owner can reject"));
    }

    String status = req.getStatus();
    if (status != null && (status.equalsIgnoreCase("Accepted") || status.equalsIgnoreCase("Rejected"))) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "Request is already decided"));
    }

    req.setStatus("Rejected");
    requestRepository.save(req);

    // Email sending removed; notifications handled via NotificationService

    try {
      String volunteerEmail = req.getVolunteer() != null ? req.getVolunteer().getVolunteerEmail() : null;
      String postTitle = post.getPostTitle() != null ? post.getPostTitle() : "(unknown)";
      if (volunteerEmail != null) {
        notificationService.createAndSend(volunteerEmail,
            "Request Rejected",
            "Your request for post: " + postTitle + " has been rejected.",
            Map.of("postId", post.getId(), "requestId", req.getId()),
            "/posts/" + post.getId());
      }
      String orgEmail = post.getOrgEmail();
      if (orgEmail != null) {
        notificationService.createAndSend(orgEmail,
            "Request Rejected",
            "You have rejected a request for post: " + postTitle + ".",
            Map.of("postId", post.getId(), "requestId", req.getId()),
            "/manage/posts/" + post.getId());
      }
    } catch (Exception ignored) {}

    return ResponseEntity.ok(Map.of("success", true));
  }

  @GetMapping("/")
  public Map<String, String> healthCheck() {
    return Map.of("message", "Volunteer Management System is running perfectly !");
  }
}
