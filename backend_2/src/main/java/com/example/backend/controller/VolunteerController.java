package com.example.backend.controller;


import com.example.backend.dto.VolunteerRequestDto;
import com.example.backend.entity.Volunteer;
import com.example.backend.entity.VolunteerPost;
import com.example.backend.entity.VolunteerRequest;
import com.example.backend.repo.VolunteerPostRepository;
import com.example.backend.repo.VolunteerRepository;
import com.example.backend.repo.VolunteerRequestRepository;
import com.example.backend.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = {"http://localhost:5173", "https://volunteer-management-sys-66dad.web.app"}, allowCredentials = "true")
@Transactional
public class VolunteerController {

  @Autowired
  private VolunteerPostRepository postRepository;

  @Autowired
  private VolunteerRequestRepository requestRepository;

  @Autowired
  private VolunteerRepository volunteerRepository;

  @Autowired
  private JwtService jwtService; // Dịch vụ xử lý JWT

  private VolunteerRequestDto toVolunteerRequestDto(VolunteerRequest request) {
    VolunteerRequestDto dto = new VolunteerRequestDto();
    dto.setId(request.getId());
    dto.setStatus(request.getStatus());

    VolunteerPost post = request.getVolunteerPost();
    if (post != null) {
      dto.setPostId(post.getId());
      dto.setPostTitle(post.getPostTitle());
      dto.setOrgEmail(post.getOrgEmail());
      dto.setDeadline(post.getDeadline() != null ? post.getDeadline().toString() : null);
      dto.setLocation(post.getLocation());
      dto.setCategory(post.getCategory());
    }

    return dto;
  }

  private String resolvePrincipalEmail(Volunteer current) {
    if (current == null) {
      return null;
    }
    String email = current.getVolunteerEmail();
    if (email != null && !email.isBlank()) {
      return email;
    }
    return current.getUsername();
  }

  // --- JWT/AUTH ENDPOINTS ---
  @PostMapping("/jwt")
  public ResponseEntity<?> createJwt(@RequestBody Map<String, String> user, HttpServletResponse response) {
    String email = user.get("email");
    if (email == null) {
      return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
    }
//thêm user vào db nếu chưa có
    Optional<Volunteer> existingVolunteer = volunteerRepository.findByVolunteerEmail(email);
    if (existingVolunteer.isEmpty()) {
      Volunteer newVolunteer = new Volunteer();
      newVolunteer.setVolunteerEmail(email);
      volunteerRepository.save(newVolunteer);
    }

    // Tạo JWT
    String token = jwtService.generateToken(email);

    // Thiết lập Cookie (Sử dụng service đã chỉnh sửa)
    // Set cookie trong Header: Set-Cookie: token=...; HttpOnly; Path=/; SameSite=None
    response.addHeader("Set-Cookie", jwtService.createCookieHeader("token", token, true));

    return ResponseEntity.ok(Map.of("success", true));
  }

  // API: /logout (Xóa Cookie)
  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpServletResponse response) {
    // Xóa cookie bằng cách đặt giá trị rỗng và maxAge=0
    response.addHeader("Set-Cookie", jwtService.createCookieHeader("token", "", false, 0));

    return ResponseEntity.ok(Map.of("success", true));
  }

  // --- VOLUNTEER POSTS ENDPOINTS ---

  // API: /volunteers (Home Page - 6 bài gần hết hạn)
  @GetMapping("/volunteers")
  public List<VolunteerPost> getLatestVolunteers() {
    return postRepository.findTop6ByOrderByDeadlineAsc();
  }

  // API: /need-volunteers (Tất cả bài, có tìm kiếm)
  @GetMapping("/need-volunteers")
  public List<VolunteerPost> getAllVolunteers(@RequestParam(required = false) String search) {
  //    System.out.println("✅ Request POST reached the Controller for /request-volunteer");
  //    System.out.println("Request data: ");
    if (search != null && !search.isEmpty()) {
      return postRepository.findByPostTitleContainingIgnoreCase(search);
    }
    return postRepository.findAll();
  }

  // API: /post/:id (Chi tiết bài)
  @GetMapping("/post/{id}")
  public ResponseEntity<VolunteerPost> getVolunteerPostDetails(@PathVariable Long id) {
    Optional<VolunteerPost> post = postRepository.findById(id);
    return post.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  // API: /add-volunteer-post
  @PostMapping("/add-volunteer-post")
  public ResponseEntity<?> addVolunteerPost(@RequestBody VolunteerPost post) {
    VolunteerPost savedPost = postRepository.save(post);
    // Tương đương với việc trả về insertedId
    return ResponseEntity.ok(Map.of("insertedId", savedPost.getId()));
  }

  // API: /update-volunteer-count/:id (Giảm số lượng)
  @PutMapping("/update-volunteer-count/{id}")
  @Transactional // Quan trọng: cần Transactional cho UPDATE query
  public ResponseEntity<?> updateVolunteerCount(@PathVariable Long id) {

    int updatedRows = postRepository.decrementVolunteerCount(id);
    if (updatedRows == 0) {
      // Có thể là không tìm thấy ID hoặc noOfVolunteer đã là 0
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Post not found or volunteer count already zero"));
    }
    return ResponseEntity.ok(Map.of("success", true, "updatedRows", updatedRows));
  }

  // API: /update-volunteer-post/:id (Hoàn thiện API bị comment)
  @PutMapping("/update-volunteer-post/{id}")
  public ResponseEntity<?> updateVolunteerPost(@PathVariable Long id, @RequestBody VolunteerPost updatedData) {
    Optional<VolunteerPost> existingPostOpt = postRepository.findById(id);

    if (!existingPostOpt.isPresent()) {
      return ResponseEntity.notFound().build();
    }

    VolunteerPost existingPost = existingPostOpt.get();
    // Cập nhật các trường
    existingPost.setPostTitle(updatedData.getPostTitle());
    existingPost.setCategory(updatedData.getCategory());
    existingPost.setDeadline(updatedData.getDeadline());
    existingPost.setLocation(updatedData.getLocation());
    existingPost.setNoOfVolunteer(updatedData.getNoOfVolunteer());
    existingPost.setThumbnail(updatedData.getThumbnail());
    existingPost.setDescription(updatedData.getDescription());
    // LƯU Ý: Không cập nhật orgEmail/orgName/noOfVolunteer ở đây (đã có API riêng cho count)

    postRepository.save(existingPost); // save hoạt động như update khi có ID
    return ResponseEntity.ok(Map.of("success", true));
  }

  // API: /my-volunteer-post/:id
  @DeleteMapping("/my-volunteer-post/{id}")
  public ResponseEntity<?> deleteVolunteerPost(@PathVariable Long id) {
    if (!postRepository.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    postRepository.deleteById(id);
    return ResponseEntity.ok(Map.of("success", true));
  }

  // API: /get-volunteer-post/:email (Hoàn thiện API bị comment - Lấy bài đăng của tôi)
  // Lưu ý: Việc kiểm tra tokenEmail !== email sẽ được thực hiện trong Spring Security Filter
  // Ở đây chỉ đơn thuần là lấy dữ liệu
  @GetMapping("/get-volunteer-post/{email}")
  public List<VolunteerPost> getMyVolunteerPosts(@PathVariable String email) {

    return postRepository.findByOrgEmail(email);
  }

  // --- VOLUNTEER REQUESTS ENDPOINTS ---

  // API: /request-volunteer (Hoàn thiện API bị comment)
  @PostMapping("/request-volunteer")
  @Transactional
  public ResponseEntity<?> requestVolunteer(
          // ⭐️ Thay thế VolunteerRequest bằng JsonNode
          @RequestBody JsonNode body,
          // ⭐️ Lấy Entity Volunteer đã xác thực từ JWT
          @AuthenticationPrincipal Volunteer currentVolunteer) throws Exception {

    // 1. KIỂM TRA XÁC THỰC

    if (currentVolunteer == null) {
      // Nếu token không hợp lệ hoặc thiếu (hoặc bị bỏ qua), SecurityContext sẽ trống.
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User must be authenticated to submit a request.");
    }

    // 2. TRÍCH XUẤT DỮ LIỆU TỪ JSONNODE
    // Lấy postId (ví dụ: body.volunteerPost.id trong payload cũ của bạn)
    JsonNode postIdNode = body.path("volunteerPost").path("id");

    // Lấy suggestion (ví dụ: body.suggestion)
    JsonNode suggestionNode = body.get("suggestion");

    // Kiểm tra tính hợp lệ của dữ liệu cần thiết
    if (postIdNode.isMissingNode() || !postIdNode.canConvertToInt() || suggestionNode.isMissingNode()) {
      return ResponseEntity.badRequest().body("Missing or invalid 'volunteerPost.id' or 'suggestion' in request body.");
    }

    Long postId = postIdNode.asLong();
    String suggestion = suggestionNode.asText();

    // 3. TẢI ENTITY VolunteerPost (Đảm bảo nó tồn tại)
    VolunteerPost post = postRepository.findById(postId).orElse(null);

    // 4. XÂY DỰNG ENTITY VolunteerRequest HOÀN CHỈNH
    VolunteerRequest newRequest = new VolunteerRequest();

    // Gán các Entity đã được quản lý
    newRequest.setVolunteerPost(post);
    newRequest.setVolunteer(currentVolunteer); // ⭐️ Gán Volunteer đã xác thực từ JWT (Fix lỗi not-null)
    System.out.println("✅ Volunteer making request: " + currentVolunteer.getUsername());
    // Gán dữ liệu khác
    newRequest.setSuggestion(suggestion);
    newRequest.setStatus("Pending"); // Thiết lập trạng thái mặc định an toàn
    newRequest.setRequestDate(LocalDateTime.now());

    // 5. LƯU
    VolunteerRequest savedRequest = requestRepository.save(newRequest);

    //
    return ResponseEntity.ok(Map.of("insertedId", savedRequest.getId()));
  }

  // API: /get-volunteer-request/:email (Hoàn thiện API bị comment - Lấy yêu cầu của tôi)
  @GetMapping("/get-volunteer-request/{email}")
  @Transactional // ⭐️ Giữ Session mở để tải LAZY Entity (fix L.I.E)
  public List<VolunteerRequestDto> getMyVolunteerRequests(@PathVariable String email) {
//    System.out.println("✅ Request POST reached the Controller for /request-volunteer");
//    System.out.println("Request data: " + email);
//    return requestRepository.findByVolunteerVolunteerEmail(email);
//    List<VolunteerRequest> requests = requestRepository.findByVolunteerVolunteerEmail(email);
//    System.out.println("✅ Found " + requests.size() + " requests for volunteer email: " + email);
//    return requests;
    // 1. Lấy email đã được xác thực
    String authenticatedEmail = email; // Lấy từ UserDetails/Volunteer

    // 2. Tải các VolunteerRequest
    List<VolunteerRequest> requests = requestRepository.findByVolunteerVolunteerEmail(authenticatedEmail);

    // 3. Chuyển đổi sang DTO
    return requests.stream().map(this::toVolunteerRequestDto).toList();
  }


  // API: /my-volunteer-request/:id (Hoàn thiện API bị comment - Hủy yêu cầu)
  @DeleteMapping("/my-volunteer-request/{id}")
  public ResponseEntity<?> removeVolunteerRequest(@PathVariable Long id) {
    if (!requestRepository.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    requestRepository.deleteById(id);
    return ResponseEntity.ok(Map.of("success", true));
  }

  @GetMapping("/get-volunteer-requests-for-org/{email}")
  public List<VolunteerRequestDto> getRequestsForOrganizer(@PathVariable String email,
                                                           @AuthenticationPrincipal Volunteer current) {
    String principalEmail = resolvePrincipalEmail(current);
    if (principalEmail == null || !principalEmail.equalsIgnoreCase(email)) {
      return List.of();
    }
    List<VolunteerRequest> requests = requestRepository.findByVolunteerPostOrgEmail(email);
    return requests.stream().map(this::toVolunteerRequestDto).toList();
  }

  @GetMapping("/org/{email}/posts-with-pending-count")
  public List<Map<String, Object>> getPostsWithPendingCounts(@PathVariable String email,
                                                             @AuthenticationPrincipal Volunteer current) {
    String principalEmail = resolvePrincipalEmail(current);
    if (principalEmail == null || !principalEmail.equalsIgnoreCase(email)) {
      return List.of();
    }
    return postRepository.findByOrgEmail(email).stream()
        .map(post -> Map.<String, Object>of(
            "id", post.getId(),
            "postTitle", post.getPostTitle(),
            "pendingCount", requestRepository.countByVolunteerPostIdAndStatusIgnoreCase(post.getId(), "Pending")
        ))
        .toList();
  }

  @GetMapping("/post/{postId}/requests")
  public Page<VolunteerRequestDto> getRequestsForPost(@PathVariable Long postId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @AuthenticationPrincipal Volunteer current) {
    Optional<VolunteerPost> postOpt = postRepository.findById(postId);
    String principalEmail = resolvePrincipalEmail(current);
    if (postOpt.isEmpty() || principalEmail == null || !principalEmail.equalsIgnoreCase(postOpt.get().getOrgEmail())) {
      return Page.empty();
    }

    final int PAGE_SIZE = 10;
    int safePage = Math.max(page, 0);
    int offset = safePage * PAGE_SIZE;

    List<VolunteerRequest> requests = requestRepository.findByVolunteerPostId(postId, PAGE_SIZE, offset);
    long total = requestRepository.countByVolunteerPostId(postId);
    List<VolunteerRequestDto> dtos = requests.stream().map(this::toVolunteerRequestDto).toList();

    return new PageImpl<>(dtos, PageRequest.of(safePage, PAGE_SIZE), total);
  }

  @PutMapping("/volunteer-request/{id}/approve")
  @Transactional
  public ResponseEntity<?> approveVolunteerRequest(@PathVariable Long id,
                                                   @AuthenticationPrincipal Volunteer current) {
    Optional<VolunteerRequest> opt = requestRepository.findByIdWithPost(id);
    if (opt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    VolunteerRequest request = opt.get();
    VolunteerPost post = request.getVolunteerPost();
    if (post == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "Missing post for request"));
    }

    Optional<VolunteerPost> refreshedPostOpt = postRepository.findById(post.getId());
    VolunteerPost refreshedPost = refreshedPostOpt.orElse(post);

    String principalEmail = resolvePrincipalEmail(current);
    if (principalEmail == null || !principalEmail.equalsIgnoreCase(refreshedPost.getOrgEmail())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("message", "Only post owner can approve"));
    }

    String currentStatus = request.getStatus();
    if (currentStatus != null && (currentStatus.equalsIgnoreCase("Accepted") || currentStatus.equalsIgnoreCase("Rejected"))) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "Request is already decided"));
    }

    Integer slots = refreshedPost.getNoOfVolunteer();
    if (slots != null && slots <= 0) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "No available slots"));
    }

    int updatedStatus = requestRepository.updateStatus(id, "Accepted");
    if (updatedStatus == 0) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("message", "Failed to update status"));
    }

    int updatedCount = postRepository.decrementVolunteerCount(refreshedPost.getId());
    if (updatedCount == 0) {
      requestRepository.updateStatus(id, currentStatus != null ? currentStatus : "Pending");
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("message", "Failed to decrement count"));
    }

    return ResponseEntity.ok(Map.of("success", true));
  }

  @PutMapping("/volunteer-request/{id}/reject")
  @Transactional
  public ResponseEntity<?> rejectVolunteerRequest(@PathVariable Long id,
                                                  @AuthenticationPrincipal Volunteer current) {
    Optional<VolunteerRequest> opt = requestRepository.findByIdWithPost(id);
    if (opt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    VolunteerRequest request = opt.get();
    VolunteerPost post = request.getVolunteerPost();
    if (post == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "Missing post for request"));
    }

    Optional<VolunteerPost> refreshedPostOpt = postRepository.findById(post.getId());
    VolunteerPost refreshedPost = refreshedPostOpt.orElse(post);

    String principalEmail = resolvePrincipalEmail(current);
    if (principalEmail == null || !principalEmail.equalsIgnoreCase(refreshedPost.getOrgEmail())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("message", "Only post owner can reject"));
    }

    String currentStatus = request.getStatus();
    if (currentStatus != null && (currentStatus.equalsIgnoreCase("Accepted") || currentStatus.equalsIgnoreCase("Rejected"))) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "Request is already decided"));
    }

    int updated = requestRepository.updateStatus(id, "Rejected");
    if (updated == 0) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("message", "Failed to update status"));
    }

    return ResponseEntity.ok(Map.of("success", true));
  }

  @GetMapping("/")
  public Map<String, String> healthCheck() {
    // Trả về một JSON response đơn giản
    return Map.of("message", "Volunteer Management System is running perfectly !");
  }
}