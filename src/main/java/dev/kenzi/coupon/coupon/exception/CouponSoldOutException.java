package dev.kenzi.coupon.coupon.exception;

import dev.kenzi.coupon.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class CouponSoldOutException extends BusinessException {

    public CouponSoldOutException() {
        super(HttpStatus.CONFLICT, "쿠폰이 모두 소진되었습니다");
    }
}
