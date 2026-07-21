package dev.kenzi.coupon.coupon.exception;

import dev.kenzi.coupon.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class AlreadyUsedCouponException extends BusinessException {

    public AlreadyUsedCouponException() {
        super(HttpStatus.CONFLICT, "이미 사용된 쿠폰입니다");
    }
}
