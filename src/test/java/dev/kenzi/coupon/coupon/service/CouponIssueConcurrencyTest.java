package dev.kenzi.coupon.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kenzi.coupon.coupon.domain.Coupon;
import dev.kenzi.coupon.coupon.dto.CouponCreateRequest;
import dev.kenzi.coupon.coupon.repository.CouponRepository;
import dev.kenzi.coupon.coupon.repository.IssuedCouponRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("concurrency")
class CouponIssueConcurrencyTest {

    private static final int TOTAL_QUANTITY = 100;
    private static final int REQUEST_COUNT = 1000;

    @Autowired
    CouponService couponService;

    @Autowired
    CouponRepository couponRepository;

    @Autowired
    IssuedCouponRepository issuedCouponRepository;

    @BeforeEach
    void cleanUp() {
        issuedCouponRepository.deleteAllInBatch();
        couponRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("동시에 1000명이 발급을 요청하면 100장 쿠폰은 정확히 100장만 발급되어야 한다")
    void issue_concurrently() throws InterruptedException {
        Long couponId = couponService.create(new CouponCreateRequest(
                "선착순 " + TOTAL_QUANTITY + "장 쿠폰",
                TOTAL_QUANTITY,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        ));

        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(REQUEST_COUNT);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        Map<String, AtomicInteger> failureReasons = new ConcurrentHashMap<>();
        long startTime = System.currentTimeMillis();

        for (long userId = 1; userId <= REQUEST_COUNT; userId++) {
            long currentUserId = userId;
            executor.submit(() -> {
                try {
                    couponService.issue(couponId, currentUserId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    failureReasons
                            .computeIfAbsent(e.getClass().getSimpleName(), key -> new AtomicInteger())
                            .incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        long elapsedMs = System.currentTimeMillis() - startTime;

        Coupon coupon = couponRepository.findById(couponId).orElseThrow();
        long actualIssuedCount = issuedCouponRepository.count();

        System.out.println("======================================");
        System.out.println("소요 시간         : " + elapsedMs + "ms");
        System.out.println("총 수량           : " + TOTAL_QUANTITY);
        System.out.println("동시 요청 수      : " + REQUEST_COUNT);
        System.out.println("성공 응답 수      : " + successCount.get());
        System.out.println("실패 응답 수      : " + failureCount.get());
        System.out.println("issuedQuantity 값 : " + coupon.getIssuedQuantity());
        System.out.println("실제 발급 내역 수 : " + actualIssuedCount);
        System.out.println("초과 발급         : " + (actualIssuedCount - TOTAL_QUANTITY) + "장");
        System.out.println("실패 원인별 집계  :");
        failureReasons.forEach((reason, count) ->
                System.out.println("  - " + reason + ": " + count.get()));
        System.out.println("======================================");

        assertThat(actualIssuedCount).isEqualTo(TOTAL_QUANTITY);
        assertThat(coupon.getIssuedQuantity()).isEqualTo(TOTAL_QUANTITY);
    }
}
