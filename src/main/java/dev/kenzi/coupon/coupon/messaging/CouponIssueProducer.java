package dev.kenzi.coupon.coupon.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponIssueProducer {

    public static final String TOPIC = "coupon-issue";

    private final KafkaTemplate<String, CouponIssueMessage> kafkaTemplate;

    public void send(CouponIssueMessage message) {
        kafkaTemplate.send(TOPIC, String.valueOf(message.userId()), message);
    }
}
