package dev.kenzi.coupon.global.error;

import dev.kenzi.coupon.user.exception.DuplicateEmailException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기.
 * 컨트롤러/서비스에서 던진 예외를 한 곳에서 HTTP 응답으로 변환한다.
 * 각 컨트롤러마다 try-catch를 반복하지 않기 위한 스프링의 표준 패턴.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 중복 이메일 → 409 Conflict */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(e.getMessage()));
    }

    /** @Valid 검증 실패 → 400 Bad Request (어느 필드가 왜 실패했는지 모아서 응답) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }
}
