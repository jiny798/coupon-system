package dev.kenzi.coupon.coupon.exception;

import dev.kenzi.coupon.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class CouponNotInPeriodException extends BusinessException {

    public CouponNotInPeriodException() {
        super(HttpStatus.BAD_REQUEST, "쿠폰 발급 가능 기간이 아닙니다");
    }
}
