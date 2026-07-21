package dev.kenzi.coupon.coupon.exception;

import dev.kenzi.coupon.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class NotCouponOwnerException extends BusinessException {

    public NotCouponOwnerException() {
        super(HttpStatus.FORBIDDEN, "본인이 발급받은 쿠폰만 사용할 수 있습니다");
    }
}
