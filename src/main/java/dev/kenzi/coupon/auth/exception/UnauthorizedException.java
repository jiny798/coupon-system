package dev.kenzi.coupon.auth.exception;

import dev.kenzi.coupon.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException() {
        super(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다");
    }
}
