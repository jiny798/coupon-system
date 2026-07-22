package dev.kenzi.coupon.coupon.exception;

import dev.kenzi.coupon.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class CouponIssueRetryExceededException extends BusinessException {

    public CouponIssueRetryExceededException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "요청이 몰려 발급에 실패했습니다. 잠시 후 다시 시도해주세요");
    }
}
