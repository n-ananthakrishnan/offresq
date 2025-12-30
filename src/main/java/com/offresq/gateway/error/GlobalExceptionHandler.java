package com.offresq.gateway.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("fieldErrors", ex.getBindingResult().getFieldErrors().stream().map(fe -> Map.of(
        "field", fe.getField(),
        "rejectedValue", fe.getRejectedValue(),
        "message", fe.getDefaultMessage()
    )).toList());

    ApiError body = new ApiError(
        Instant.now(),
        400,
        "Bad Request",
        "Validation failed",
        req.getRequestURI(),
        details
    );
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleAny(Exception ex, HttpServletRequest req) {
    ApiError body = new ApiError(
        Instant.now(),
        500,
        "Internal Server Error",
        ex.getMessage(),
        req.getRequestURI(),
        Map.of()
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}