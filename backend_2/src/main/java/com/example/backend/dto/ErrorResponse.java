package com.example.backend.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Mẫu JSON cho mọi error response.
 *
 * Ví dụ trả về:
 * {
 *   "timestamp": "2025-11-20T15:30:00",
 *   "path": "/api/volunteers",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "postTitle is required",
 *   "code": "BAD_REQUEST"
 * }
 */
@Data
@Builder
public class ErrorResponse {

  private LocalDateTime timestamp;
  private String path;
  private int status;
  private String error;
  private String message;
  private String code;
}
