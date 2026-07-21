package dev.kenzi.coupon.coupon.exception;

import dev.kenzi.coupon.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class IssuedCouponNotFoundException extends BusinessException {

    public IssuedCouponNotFoundException(Long issuedCouponId) {
        super(HttpStatus.NOT_FOUND, "발급된 쿠폰을 찾을 수 없습니다: id=" + issuedCouponId);
    }
}
