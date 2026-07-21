package dev.kenzi.coupon.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kenzi.coupon.coupon.exception.CouponNotInPeriodException;
import dev.kenzi.coupon.coupon.exception.CouponSoldOutException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CouponTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 21, 12, 0);

    private Coupon coupon(int totalQuantity) {
        return Coupon.builder()
                .name("선착순 쿠폰")
                .totalQuantity(totalQuantity)
                .validFrom(NOW.minusDays(1))
                .validUntil(NOW.plusDays(1))
                .build();
    }

    @Test
    @DisplayName("발급하면 발급 수량이 1 증가한다")
    void issue_increases_issued_quantity() {
        Coupon coupon = coupon(10);

        coupon.issue(NOW);

        assertThat(coupon.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("총 수량만큼 발급되면 더 이상 발급할 수 없다")
    void issue_rejects_when_sold_out() {
        Coupon coupon = coupon(2);
        coupon.issue(NOW);
        coupon.issue(NOW);

        assertThatThrownBy(() -> coupon.issue(NOW))
                .isInstanceOf(CouponSoldOutException.class);
        assertThat(coupon.getIssuedQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("유효기간 시작 전에는 발급할 수 없다")
    void issue_rejects_before_period() {
        Coupon coupon = coupon(10);

        assertThatThrownBy(() -> coupon.issue(NOW.minusDays(2)))
                .isInstanceOf(CouponNotInPeriodException.class);
    }

    @Test
    @DisplayName("유효기간이 지나면 발급할 수 없다")
    void issue_rejects_after_period() {
        Coupon coupon = coupon(10);

        assertThatThrownBy(() -> coupon.issue(NOW.plusDays(2)))
                .isInstanceOf(CouponNotInPeriodException.class);
    }
}
