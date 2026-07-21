package dev.kenzi.coupon.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kenzi.coupon.coupon.exception.AlreadyUsedCouponException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IssuedCouponTest {

    private IssuedCoupon issuedCoupon() {
        Coupon coupon = Coupon.builder()
                .name("선착순 쿠폰")
                .totalQuantity(10)
                .validFrom(LocalDateTime.of(2026, 1, 1, 0, 0))
                .validUntil(LocalDateTime.of(2026, 12, 31, 23, 59))
                .build();
        return IssuedCoupon.builder()
                .coupon(coupon)
                .userId(1L)
                .build();
    }

    @Test
    @DisplayName("사용하면 사용 시각이 기록된다")
    void use_marks_used() {
        IssuedCoupon issuedCoupon = issuedCoupon();

        issuedCoupon.use();

        assertThat(issuedCoupon.isUsed()).isTrue();
        assertThat(issuedCoupon.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 사용한 쿠폰은 다시 사용할 수 없다")
    void use_rejects_already_used() {
        IssuedCoupon issuedCoupon = issuedCoupon();
        issuedCoupon.use();

        assertThatThrownBy(issuedCoupon::use)
                .isInstanceOf(AlreadyUsedCouponException.class);
    }
}
