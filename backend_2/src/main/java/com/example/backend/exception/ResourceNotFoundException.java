package com.example.backend.exception;

/**
 * Dùng cho các lỗi 404 - không tìm thấy resource (post, volunteer,...)
 */
public class ResourceNotFoundException extends BusinessException {

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
