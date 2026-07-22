package dev.kenzi.coupon.coupon.service;

import dev.kenzi.coupon.coupon.exception.CouponIssueRetryExceededException;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OptimisticCouponIssueFacade {

    private static final int MAX_RETRY = 300;
    private static final long RETRY_DELAY_MS = 20;

    private final CouponService couponService;
    private final AtomicLong totalRetryCount = new AtomicLong();

    public Long issue(Long couponId, Long userId) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                return couponService.issue(couponId, userId);
            } catch (OptimisticLockingFailureException | CannotAcquireLockException e) {
                totalRetryCount.incrementAndGet();
                sleep();
            }
        }
        throw new CouponIssueRetryExceededException();
    }

    public long getTotalRetryCount() {
        return totalRetryCount.get();
    }

    private void sleep() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
