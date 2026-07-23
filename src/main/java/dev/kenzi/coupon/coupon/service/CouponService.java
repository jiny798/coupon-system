package dev.kenzi.coupon.coupon.service;

import dev.kenzi.coupon.coupon.domain.Coupon;
import dev.kenzi.coupon.coupon.domain.IssuedCoupon;
import dev.kenzi.coupon.coupon.dto.CouponCreateRequest;
import dev.kenzi.coupon.coupon.dto.IssuedCouponResponse;
import dev.kenzi.coupon.coupon.exception.CouponNotFoundException;
import dev.kenzi.coupon.coupon.exception.DuplicateCouponIssueException;
import dev.kenzi.coupon.coupon.exception.InvalidCouponPeriodException;
import dev.kenzi.coupon.coupon.exception.IssuedCouponNotFoundException;
import dev.kenzi.coupon.coupon.exception.NotCouponOwnerException;
import dev.kenzi.coupon.coupon.repository.CouponRepository;
import dev.kenzi.coupon.coupon.repository.IssuedCouponRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    @Transactional
    public Long create(CouponCreateRequest request) {
        if (!request.validFrom().isBefore(request.validUntil())) {
            throw new InvalidCouponPeriodException();
        }

        Coupon coupon = Coupon.builder()
                .name(request.name())
                .totalQuantity(request.totalQuantity())
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .build();

        return couponRepository.save(coupon).getId();
    }

    @Transactional
    public Long issue(Long couponId, Long userId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponNotFoundException(couponId));

        if (issuedCouponRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw new DuplicateCouponIssueException();
        }

        coupon.issue(LocalDateTime.now());

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .coupon(coupon)
                .userId(userId)
                .build();

        try {
            return issuedCouponRepository.save(issuedCoupon).getId();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateCouponIssueException();
        }
    }

    @Transactional
    public Long recordIssuance(Long couponId, Long userId) {
        Coupon coupon = couponRepository.getReferenceById(couponId);
        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .coupon(coupon)
                .userId(userId)
                .build();
        try {
            return issuedCouponRepository.save(issuedCoupon).getId();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateCouponIssueException();
        }
    }

    @Transactional(readOnly = true)
    public List<IssuedCouponResponse> getMyCoupons(Long userId) {
        return issuedCouponRepository.findAllWithCouponByUserId(userId).stream()
                .map(IssuedCouponResponse::from)
                .toList();
    }

    @Transactional
    public void use(Long issuedCouponId, Long userId) {
        IssuedCoupon issuedCoupon = issuedCouponRepository.findById(issuedCouponId)
                .orElseThrow(() -> new IssuedCouponNotFoundException(issuedCouponId));

        if (!issuedCoupon.getUserId().equals(userId)) {
            throw new NotCouponOwnerException();
        }

        issuedCoupon.use();
    }
}
