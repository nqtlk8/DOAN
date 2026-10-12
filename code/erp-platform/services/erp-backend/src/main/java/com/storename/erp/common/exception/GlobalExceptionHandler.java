package com.storename.erp.common.exception;

import com.storename.erp.common.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(org.springframework.security.authentication.BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Bad credentials", java.util.List.of("BAD_CREDENTIALS")));
    }

    @ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledException(org.springframework.security.authentication.DisabledException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), java.util.List.of("USER_INACTIVE")));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        // Loi du lieu dau vao -> 400 (khong dung 401 de frontend khong hieu nham la het phien).
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), java.util.List.of("INVALID_ARGUMENT")));
    }

    @ExceptionHandler(BusinessConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessConflictException(BusinessConflictException ex) {
        // Vi pham quy tac nghiep vu (vd khong du ton kho khi ban) -> 409, message hien thang cho nguoi dung.
        log.warn("Business conflict [{}]: {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), java.util.List.of(ex.getCode())));
    }

    @ExceptionHandler(io.jsonwebtoken.ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpiredJwtException(io.jsonwebtoken.ExpiredJwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Token expired. Please log in again.", java.util.List.of("TOKEN_EXPIRED")));
    }

    @ExceptionHandler(io.jsonwebtoken.JwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwtException(io.jsonwebtoken.JwtException ex) {
        // Chi tiet (revoked, reuse, sai loai token...) chi ghi log; client nhan thong diep chung.
        log.warn("Rejected token: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid or revoked token. Please log in again.", java.util.List.of("INVALID_TOKEN")));
    }

    @ExceptionHandler(com.storename.erp.identity.domain.exception.SigningKeyNotConfiguredException.class)
    public ResponseEntity<ApiResponse<Void>> handleSigningKeyNotConfigured(com.storename.erp.identity.domain.exception.SigningKeyNotConfiguredException ex) {
        log.error("JWT signing key not configured: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Authentication service is not configured", java.util.List.of("SIGNING_KEY_NOT_CONFIGURED")));
    }

    @ExceptionHandler(org.springframework.data.redis.RedisConnectionFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleRedisConnectionFailure(org.springframework.data.redis.RedisConnectionFailureException ex) {
        // Redis (HQ) luu trang thai refresh/thu hoi token: loi tam thoi -> 503, frontend khong xoa phien.
        log.error("Redis unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Authentication store is temporarily unavailable. Please retry.", java.util.List.of("AUTH_STORE_UNAVAILABLE")));
    }

    @ExceptionHandler(com.storename.erp.identity.domain.exception.NoRoleAssignedException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoRoleAssignedException(com.storename.erp.identity.domain.exception.NoRoleAssignedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), java.util.List.of("NO_ROLE_ASSIGNED")));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Resource not found", java.util.List.of("NOT_FOUND")));
    }

    @ExceptionHandler(com.storename.erp.common.exception.ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(com.storename.erp.common.exception.ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), java.util.List.of("NOT_FOUND")));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityException(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage(), java.util.List.of("FORBIDDEN")));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.error(ex.getReason(), java.util.List.of(ex.getStatusCode().toString())));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        // Vi pham rang buoc CSDL (unique, khoa ngoai, do dai cot) con lot qua kiem tra nghiep vu -> 409.
        // Chi tiet SQL chi ghi log, khong tra ve client.
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Data conflicts with an existing record or a database constraint",
                        java.util.List.of("DATA_INTEGRITY_VIOLATION")));
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailure(org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
        // Ban ghi da bi nguoi khac sua (@Version lech) -> 409, client tai lai roi thu lai.
        log.warn("Optimistic locking failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("The record was modified by another request. Please reload and try again.",
                        java.util.List.of("CONCURRENT_UPDATE")));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        // Vd DELETE /api/v1/branches/{id}: khong co API xoa -> 405 thay vi 500.
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("Method " + ex.getMethod() + " is not supported for this endpoint",
                        java.util.List.of("METHOD_NOT_ALLOWED")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Internal server error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error", java.util.List.of("INTERNAL_ERROR")));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied: " + ex.getMessage(), java.util.List.of("FORBIDDEN")));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMsg = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Validation failed");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorMsg, java.util.List.of("VALIDATION_ERROR")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Malformed JSON request", java.util.List.of("MALFORMED_JSON")));
    }
}
