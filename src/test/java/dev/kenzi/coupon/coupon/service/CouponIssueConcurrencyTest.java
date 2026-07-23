package dev.kenzi.coupon.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kenzi.coupon.coupon.dto.CouponCreateRequest;
import dev.kenzi.coupon.coupon.repository.CouponRepository;
import dev.kenzi.coupon.coupon.repository.IssuedCouponRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
@Tag("concurrency")
class CouponIssueConcurrencyTest {

    private static final int TOTAL_QUANTITY = 100;
    private static final int REQUEST_COUNT = 1000;

    @Autowired
    CouponService couponService;

    @Autowired
    RedisCouponIssueFacade redisFacade;

    @Autowired
    CouponRepository couponRepository;

    @Autowired
    IssuedCouponRepository issuedCouponRepository;

    @Autowired
    StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanUp() {
        issuedCouponRepository.deleteAllInBatch();
        couponRepository.deleteAllInBatch();
        Objects.requireNonNull(redisTemplate.getConnectionFactory())
                .getConnection()
                .serverCommands()
                .flushDb();
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
                    redisFacade.issue(couponId, currentUserId);
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

        long actualIssuedCount = issuedCouponRepository.count();
        String redisCount = redisTemplate.opsForValue()
                .get(RedisCouponIssueFacade.issuedCountKey(couponId));
        Long redisUserCount = redisTemplate.opsForSet()
                .size(RedisCouponIssueFacade.issuedUsersKey(couponId));

        System.out.println("======================================");
        System.out.println("소요 시간         : " + elapsedMs + "ms");
        System.out.println("총 수량           : " + TOTAL_QUANTITY);
        System.out.println("동시 요청 수      : " + REQUEST_COUNT);
        System.out.println("성공 응답 수      : " + successCount.get());
        System.out.println("실패 응답 수      : " + failureCount.get());
        System.out.println("Redis 발급 카운트 : " + redisCount);
        System.out.println("Redis 유저 셋 크기: " + redisUserCount);
        System.out.println("실제 발급 내역 수 : " + actualIssuedCount);
        System.out.println("초과 발급         : " + (actualIssuedCount - TOTAL_QUANTITY) + "장");
        System.out.println("실패 원인별 집계  :");
        failureReasons.forEach((reason, count) ->
                System.out.println("  - " + reason + ": " + count.get()));
        System.out.println("======================================");

        assertThat(actualIssuedCount).isEqualTo(TOTAL_QUANTITY);
        assertThat(Long.parseLong(Objects.requireNonNull(redisCount))).isEqualTo(TOTAL_QUANTITY);
        assertThat(redisUserCount).isEqualTo(TOTAL_QUANTITY);
    }
}
