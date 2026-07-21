package dev.kenzi.coupon.auth.exception;

import dev.kenzi.coupon.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidLoginException extends BusinessException {

    public InvalidLoginException() {
        super(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다");
    }
}
