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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.example.backend.repo.VolunteerRequestRepository;
import com.example.backend.dto.VolunteerPostDto;

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
  public List<VolunteerPostDto> getAllVolunteers(@RequestParam(required = false) String search) {
    return postService.getAllVolunteers(search);
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

  String email = currentVolunteer.getVolunteerEmail();
  if (email == null || email.isBlank()) {
    email = currentVolunteer.getUsername();
  }
  var managedVolunteerOpt = volunteerRepository.findByVolunteerEmail(email);
  if (managedVolunteerOpt.isEmpty()) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body("Authenticated volunteer not found: " + email);
  }
  Volunteer managedVolunteer = managedVolunteerOpt.get();

  Long insertedId = requestService.requestVolunteer(body, managedVolunteer);
  return ResponseEntity.ok(Map.of("insertedId", insertedId));
}


  // Lấy yêu cầu của tôi
  @GetMapping("/get-volunteer-request/{email}")
  public List<VolunteerRequestDto> getMyVolunteerRequests(@PathVariable String email) {
    return requestService.getMyVolunteerRequests(email);
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

  @GetMapping("/")
  public Map<String, String> healthCheck() {
    return Map.of("message", "Volunteer Management System is running perfectly !");
  }
}
