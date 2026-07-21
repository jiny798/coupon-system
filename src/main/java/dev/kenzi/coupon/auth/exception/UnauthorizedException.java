package dev.kenzi.coupon.auth.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("유효하지 않은 인증 정보입니다");
    }
}
