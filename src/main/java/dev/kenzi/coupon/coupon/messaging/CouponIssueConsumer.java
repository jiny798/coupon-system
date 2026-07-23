package dev.kenzi.coupon.coupon.messaging;

import dev.kenzi.coupon.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {

    private final CouponService couponService;

    @KafkaListener(topics = CouponIssueProducer.TOPIC, groupId = "coupon-issue-consumer")
    public void consume(CouponIssueMessage message) {
        log.info("쿠폰 발급 메시지 수신: couponId={}, userId={}", message.couponId(), message.userId());
        couponService.recordIssuance(message.couponId(), message.userId());
    }
}
