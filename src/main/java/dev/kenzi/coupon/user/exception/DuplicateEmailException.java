package dev.kenzi.coupon.user.exception;

import dev.kenzi.coupon.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException(String email) {
        super(HttpStatus.CONFLICT, "이미 가입된 이메일입니다: " + email);
    }
}
