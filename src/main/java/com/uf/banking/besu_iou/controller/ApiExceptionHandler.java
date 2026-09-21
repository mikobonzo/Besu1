package com.uf.banking.besu_iou.controller;

import com.uf.banking.besu_iou.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", fields);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> badRequest(IllegalArgumentException ex) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> state(IllegalStateException ex) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "BLOCKCHAIN_UNAVAILABLE", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "The request could not be completed", Map.of());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code,
            String message, Map<String, String> fields) {
        return ResponseEntity.status(status).body(
                new ApiError(Instant.now(), status.value(), code, message, fields));
    }
}
