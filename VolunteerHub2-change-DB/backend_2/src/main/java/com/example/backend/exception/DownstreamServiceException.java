package com.example.backend.exception;

public class DownstreamServiceException extends BusinessException {

  public DownstreamServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
