package dev.kenzi.coupon.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dev.kenzi.coupon.coupon.domain.Coupon;
import dev.kenzi.coupon.coupon.domain.IssuedCoupon;
import dev.kenzi.coupon.coupon.dto.CouponCreateRequest;
import dev.kenzi.coupon.coupon.exception.CouponNotFoundException;
import dev.kenzi.coupon.coupon.exception.DuplicateCouponIssueException;
import dev.kenzi.coupon.coupon.exception.InvalidCouponPeriodException;
import dev.kenzi.coupon.coupon.exception.NotCouponOwnerException;
import dev.kenzi.coupon.coupon.repository.CouponRepository;
import dev.kenzi.coupon.coupon.repository.IssuedCouponRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    CouponRepository couponRepository;

    @Mock
    IssuedCouponRepository issuedCouponRepository;

    @InjectMocks
    CouponService couponService;

    private Coupon coupon(int totalQuantity) {
        Coupon coupon = Coupon.builder()
                .name("선착순 쿠폰")
                .totalQuantity(totalQuantity)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(1))
                .build();
        ReflectionTestUtils.setField(coupon, "id", 1L);
        return coupon;
    }

    @Test
    @DisplayName("유효기간 시작일이 종료일보다 늦으면 생성할 수 없다")
    void create_rejects_invalid_period() {
        CouponCreateRequest request = new CouponCreateRequest(
                "선착순 쿠폰", 100,
                LocalDateTime.of(2026, 12, 31, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );

        assertThatThrownBy(() -> couponService.create(request))
                .isInstanceOf(InvalidCouponPeriodException.class);
        verify(couponRepository, never()).save(any());
    }

    @Test
    @DisplayName("발급에 성공하면 발급 수량이 증가하고 발급 내역이 저장된다")
    void issue_succeeds() {
        Coupon coupon = coupon(10);
        given(couponRepository.findById(1L)).willReturn(Optional.of(coupon));
        given(issuedCouponRepository.existsByCouponIdAndUserId(1L, 1L)).willReturn(false);
        given(issuedCouponRepository.save(any(IssuedCoupon.class))).willAnswer(invocation -> {
            IssuedCoupon issuedCoupon = invocation.getArgument(0);
            ReflectionTestUtils.setField(issuedCoupon, "id", 100L);
            return issuedCoupon;
        });

        Long issuedCouponId = couponService.issue(1L, 1L);

        assertThat(issuedCouponId).isEqualTo(100L);
        assertThat(coupon.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰은 발급할 수 없다")
    void issue_rejects_unknown_coupon() {
        given(couponRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.issue(99L, 1L))
                .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    @DisplayName("이미 발급받은 유저는 다시 발급받을 수 없다")
    void issue_rejects_duplicate() {
        Coupon coupon = coupon(10);
        given(couponRepository.findById(1L)).willReturn(Optional.of(coupon));
        given(issuedCouponRepository.existsByCouponIdAndUserId(1L, 1L)).willReturn(true);

        assertThatThrownBy(() -> couponService.issue(1L, 1L))
                .isInstanceOf(DuplicateCouponIssueException.class);
        verify(issuedCouponRepository, never()).save(any());
        assertThat(coupon.getIssuedQuantity()).isZero();
    }

    @Test
    @DisplayName("남이 발급받은 쿠폰은 사용할 수 없다")
    void use_rejects_not_owner() {
        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .coupon(coupon(10))
                .userId(1L)
                .build();
        given(issuedCouponRepository.findById(100L)).willReturn(Optional.of(issuedCoupon));

        assertThatThrownBy(() -> couponService.use(100L, 2L))
                .isInstanceOf(NotCouponOwnerException.class);
    }
}
