package com.example.backend.exception;

public class RateLimitExceededException extends BusinessException {

  public RateLimitExceededException(String message) {
    super(message);
  }
}
