package dev.kenzi.coupon.coupon.exception;

import dev.kenzi.coupon.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class CouponNotFoundException extends BusinessException {

    public CouponNotFoundException(Long couponId) {
        super(HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다: id=" + couponId);
    }
}
