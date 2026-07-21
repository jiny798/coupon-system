package dev.kenzi.coupon.coupon.exception;

import dev.kenzi.coupon.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidCouponPeriodException extends BusinessException {

    public InvalidCouponPeriodException() {
        super(HttpStatus.BAD_REQUEST, "유효기간 시작일은 종료일보다 이전이어야 합니다");
    }
}
