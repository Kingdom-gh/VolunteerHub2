package com.example.backend.exception;

/**
 * Dùng khi rate limit bị vượt quá (sau này kết hợp Bucket4j).
 */
public class RateLimitExceededException extends BusinessException {

  public RateLimitExceededException(String message) {
    super(message);
  }
}
