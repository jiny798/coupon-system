package dev.kenzi.coupon.coupon.service;

import dev.kenzi.coupon.coupon.domain.Coupon;
import dev.kenzi.coupon.coupon.exception.CouponNotFoundException;
import dev.kenzi.coupon.coupon.exception.CouponNotInPeriodException;
import dev.kenzi.coupon.coupon.exception.CouponSoldOutException;
import dev.kenzi.coupon.coupon.exception.DuplicateCouponIssueException;
import dev.kenzi.coupon.coupon.repository.CouponRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisCouponIssueFacade {

    private final CouponRepository couponRepository;
    private final CouponService couponService;
    private final StringRedisTemplate redisTemplate;

    public Long issue(Long couponId, Long userId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponNotFoundException(couponId));
        validatePeriod(coupon);

        String usersKey = issuedUsersKey(couponId);
        String countKey = issuedCountKey(couponId);
        String userValue = String.valueOf(userId);

        // 중복 발급 확인
        Long added = redisTemplate.opsForSet().add(usersKey, userValue);
        if (added == null || added == 0) {
            throw new DuplicateCouponIssueException();
        }

        // 특정쿠폰 사용 수량 증가
        Long issuedNumber = redisTemplate.opsForValue().increment(countKey);
        if (issuedNumber == null || issuedNumber > coupon.getTotalQuantity()) {
            redisTemplate.opsForValue().decrement(countKey); // 순번표 반납
            redisTemplate.opsForSet().remove(usersKey, userValue); // 명단 제거
            throw new CouponSoldOutException();
        }

        return couponService.recordIssuance(couponId, userId);
    }

    public static String issuedUsersKey(Long couponId) {
        return "coupon:" + couponId + ":issued-users";
    }

    public static String issuedCountKey(Long couponId) {
        return "coupon:" + couponId + ":issued-count";
    }

    private void validatePeriod(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
            throw new CouponNotInPeriodException();
        }
    }
}
