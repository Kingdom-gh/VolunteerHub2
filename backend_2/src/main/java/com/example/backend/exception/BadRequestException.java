package com.example.backend.exception;

/**
 * Dùng cho các lỗi 400 - request sai, thiếu field, format sai,...
 */
public class BadRequestException extends BusinessException {

  public BadRequestException(String message) {
    super(message);
  }
}
