package com.example.backend.exception;

import com.example.backend.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Toàn bộ exception ném ra trong hệ thống sẽ đi qua đây
 * và được map thành 1 dạng JSON chuẩn: ErrorResponse.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private ErrorResponse buildErrorResponse(
      HttpServletRequest request,
      HttpStatus status,
      String code,
      String message
  ) {
    return ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .path(request.getRequestURI())
        .status(status.value())
        .error(status.getReasonPhrase())
        .message(message)
        .code(code)
        .build();
  }

  // ====== 400 - BAD REQUEST ======

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(
      BadRequestException ex,
      HttpServletRequest request
  ) {
    ErrorResponse body = buildErrorResponse(
        request,
        HttpStatus.BAD_REQUEST,
        "BAD_REQUEST",
        ex.getMessage()
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  /**
   * Để an toàn, map cả IllegalArgumentException (trường hợp
   * nào đó chưa kịp đổi sang BadRequestException).
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(
      IllegalArgumentException ex,
      HttpServletRequest request
  ) {
    ErrorResponse body = buildErrorResponse(
        request,
        HttpStatus.BAD_REQUEST,
        "BAD_REQUEST",
        ex.getMessage()
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  // ====== 404 - NOT FOUND ======

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(
      ResourceNotFoundException ex,
      HttpServletRequest request
  ) {
    ErrorResponse body = buildErrorResponse(
        request,
        HttpStatus.NOT_FOUND,
        "NOT_FOUND",
        ex.getMessage()
    );
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  // ====== 429 - RATE LIMIT ======

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<ErrorResponse> handleRateLimit(
      RateLimitExceededException ex,
      HttpServletRequest request
  ) {
    ErrorResponse body = buildErrorResponse(
        request,
        HttpStatus.TOO_MANY_REQUESTS,
        "RATE_LIMIT",
        ex.getMessage()
    );
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
  }

  // ====== BusinessException chung ======

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusiness(
      BusinessException ex,
      HttpServletRequest request
  ) {
    // Mặc định map về 400, nếu sau này muốn tách code thì sửa lại ở từng subclass
    ErrorResponse body = buildErrorResponse(
        request,
        HttpStatus.BAD_REQUEST,
        "BUSINESS_ERROR",
        ex.getMessage()
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  // 503 - Lỗi hệ thống phụ trợ (DB/service khác) được wrap lại bởi Resilience4j fallback
  @ExceptionHandler(DownstreamServiceException.class)
  public ResponseEntity<ErrorResponse> handleDownstream(
      DownstreamServiceException ex,
      HttpServletRequest request
  ) {
    log.error("Downstream service error for request {}", request.getRequestURI(), ex);
    ErrorResponse body = buildErrorResponse(
        request,
        HttpStatus.SERVICE_UNAVAILABLE,
        "DOWNSTREAM_UNAVAILABLE",
        ex.getMessage()
    );
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
  }

  // 503 - CircuitBreaker đã mở (không cho gọi xuống service nữa)
  @ExceptionHandler(CallNotPermittedException.class)
  public ResponseEntity<ErrorResponse> handleCircuitOpen(
      CallNotPermittedException ex,
      HttpServletRequest request
  ) {
    log.warn("Circuit open for request {}", request.getRequestURI(), ex);
    ErrorResponse body = buildErrorResponse(
        request,
        HttpStatus.SERVICE_UNAVAILABLE,
        "CIRCUIT_OPEN",
        "Service is temporarily unavailable. Please try again later."
    );
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
  }

  // ====== Fallback cuối cùng cho mọi Exception không lường trước ======

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(
      Exception ex,
      HttpServletRequest request
  ) {
    log.error("Unhandled exception for request {}", request.getRequestURI(), ex);
    ErrorResponse body = buildErrorResponse(
        request,
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "Something went wrong"
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}
