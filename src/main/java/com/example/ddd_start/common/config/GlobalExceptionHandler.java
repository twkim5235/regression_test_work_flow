package com.example.ddd_start.common.config;

import com.example.ddd_start.common.domain.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("잘못된 요청: {}", e.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NoMemberFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoMemberFoundException(NoMemberFoundException e) {
        log.warn("회원을 찾을 수 없음: {}", e.getMessage());
        return createErrorResponse(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");
    }

    @ExceptionHandler(NoOrderException.class)
    public ResponseEntity<Map<String, Object>> handleNoOrderException(NoOrderException e) {
        log.warn("주문을 찾을 수 없음: {}", e.getMessage());
        return createErrorResponse(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다.");
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmailException(DuplicateEmailException e) {
        log.warn("중복된 이메일: {}", e.getMessage());
        return createErrorResponse(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateUsernameException(DuplicateUsernameException e) {
        log.warn("중복된 사용자명: {}", e.getMessage());
        return createErrorResponse(HttpStatus.CONFLICT, "이미 사용 중인 사용자명입니다.");
    }

    @ExceptionHandler(PasswordNotMatchException.class)
    public ResponseEntity<Map<String, Object>> handlePasswordNotMatchException(PasswordNotMatchException e) {
        log.warn("비밀번호 불일치: {}", e.getMessage());
        return createErrorResponse(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");
    }

    @ExceptionHandler(ValidationErrorException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrorException(ValidationErrorException e) {
        log.warn("유효성 검증 실패: {}", e.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(VersionConflictException.class)
    public ResponseEntity<Map<String, Object>> handleVersionConflictException(VersionConflictException e) {
        log.warn("버전 충돌: {}", e.getMessage());
        return createErrorResponse(HttpStatus.CONFLICT, "다른 사용자가 이미 수정했습니다. 다시 시도해주세요.");
    }

    @ExceptionHandler(StoreBlockedException.class)
    public ResponseEntity<Map<String, Object>> handleStoreBlockedException(StoreBlockedException e) {
        log.warn("차단된 스토어: {}", e.getMessage());
        return createErrorResponse(HttpStatus.FORBIDDEN, "현재 이용할 수 없는 스토어입니다.");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNoSuchElementException(NoSuchElementException e) {
        log.warn("리소스를 찾을 수 없음: {}", e.getMessage());
        return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("서버 오류 발생: ", e);
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(HttpStatus status, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);
        return ResponseEntity.status(status).body(errorResponse);
    }
}
