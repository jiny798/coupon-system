package dev.kenzi.coupon.coupon.repository;

import dev.kenzi.coupon.coupon.domain.IssuedCoupon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssuedCouponRepository extends JpaRepository<IssuedCoupon, Long> {

    boolean existsByCouponIdAndUserId(Long couponId, Long userId);

    @Query("select ic from IssuedCoupon ic join fetch ic.coupon where ic.userId = :userId order by ic.issuedAt desc")
    List<IssuedCoupon> findAllWithCouponByUserId(@Param("userId") Long userId);
}
