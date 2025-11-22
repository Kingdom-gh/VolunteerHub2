package com.example.backend.exception;

/**
 * Base cho các exception nghiệp vụ trong hệ thống.
 * Có thể dùng sau này cho logging / phân loại lỗi.
 */
public class BusinessException extends RuntimeException {

  public BusinessException(String message) {
    super(message);
  }

  public BusinessException(String message, Throwable cause) {
    super(message, cause);
  }
}