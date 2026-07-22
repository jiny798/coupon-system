package dev.kenzi.coupon.coupon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SynchronizedCouponIssueFacade {

    private final CouponService couponService;

    public synchronized Long issue(Long couponId, Long userId) {
        return couponService.issue(couponId, userId);
    }
}
