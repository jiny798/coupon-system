package dev.kenzi.coupon.coupon.domain;

import dev.kenzi.coupon.coupon.exception.AlreadyUsedCouponException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "issued_coupons",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_issued_coupons_coupon_user",
                columnNames = {"coupon_id", "user_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuedCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    private LocalDateTime usedAt;

    @Builder
    private IssuedCoupon(Coupon coupon, Long userId) {
        this.coupon = coupon;
        this.userId = userId;
    }

    @PrePersist
    void prePersist() {
        this.issuedAt = LocalDateTime.now();
    }

    public void use() {
        if (usedAt != null) {
            throw new AlreadyUsedCouponException();
        }
        this.usedAt = LocalDateTime.now();
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}
