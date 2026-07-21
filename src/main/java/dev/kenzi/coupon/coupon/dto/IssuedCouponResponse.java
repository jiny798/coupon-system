package dev.kenzi.coupon.coupon.dto;

import dev.kenzi.coupon.coupon.domain.IssuedCoupon;
import java.time.LocalDateTime;

public record IssuedCouponResponse(
        Long id,
        Long couponId,
        String couponName,
        LocalDateTime issuedAt,
        LocalDateTime usedAt,
        boolean used
) {

    public static IssuedCouponResponse from(IssuedCoupon issuedCoupon) {
        return new IssuedCouponResponse(
                issuedCoupon.getId(),
                issuedCoupon.getCoupon().getId(),
                issuedCoupon.getCoupon().getName(),
                issuedCoupon.getIssuedAt(),
                issuedCoupon.getUsedAt(),
                issuedCoupon.isUsed()
        );
    }
}
