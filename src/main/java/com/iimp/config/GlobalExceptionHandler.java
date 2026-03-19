package com.iimp.config;

import com.iimp.dto.DashboardDtos;
import com.iimp.exception.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<DashboardDtos.ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(DashboardDtos.ErrorResponse.of(404, ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<DashboardDtos.ErrorResponse> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(DashboardDtos.ErrorResponse.of(400, ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<DashboardDtos.ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(DashboardDtos.ErrorResponse.of(403, ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<DashboardDtos.ErrorResponse> handleSpringAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(DashboardDtos.ErrorResponse.of(403, "Access denied: insufficient role"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<DashboardDtos.ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(DashboardDtos.ErrorResponse.of(401, ex.getMessage()));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<DashboardDtos.ErrorResponse> handleDisabled(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(DashboardDtos.ErrorResponse.of(401, ex.getMessage()));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<DashboardDtos.ErrorResponse> handleLocked(LockedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(DashboardDtos.ErrorResponse.of(401, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DashboardDtos.ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(DashboardDtos.ErrorResponse.of(400, "Validation failed: " + errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DashboardDtos.ErrorResponse> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(DashboardDtos.ErrorResponse.of(500, "Internal error: " + ex.getMessage()));
    }
}
