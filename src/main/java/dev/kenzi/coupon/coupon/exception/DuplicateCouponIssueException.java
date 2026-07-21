package dev.kenzi.coupon.coupon.exception;

import dev.kenzi.coupon.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class DuplicateCouponIssueException extends BusinessException {

    public DuplicateCouponIssueException() {
        super(HttpStatus.CONFLICT, "이미 발급받은 쿠폰입니다");
    }
}
