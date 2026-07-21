package dev.kenzi.coupon.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * record: 불변 DTO를 한 줄로 정의. request.email() 처럼 꺼내 쓴다.
 * 검증 애노테이션은 컨트롤러의 @Valid가 트리거하고,
 * 실패 시 MethodArgumentNotValidException → GlobalExceptionHandler에서 400 처리.
 */
public record UserSignupRequest(

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "이메일 형식이 올바르지 않습니다")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다")
        String password,

        @NotBlank(message = "이름은 필수입니다")
        String name
) {
}
