package dev.kenzi.coupon.coupon.domain;

import dev.kenzi.coupon.coupon.exception.CouponNotInPeriodException;
import dev.kenzi.coupon.coupon.exception.CouponSoldOutException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int issuedQuantity;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    @Builder
    private Coupon(String name, int totalQuantity, LocalDateTime validFrom, LocalDateTime validUntil) {
        this.name = name;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void issue(LocalDateTime now) {
        if (now.isBefore(validFrom) || now.isAfter(validUntil)) {
            throw new CouponNotInPeriodException();
        }
        if (issuedQuantity >= totalQuantity) {
            throw new CouponSoldOutException();
        }
        this.issuedQuantity++;
    }
}
