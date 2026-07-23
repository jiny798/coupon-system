package dev.kenzi.coupon.global.config;

import dev.kenzi.coupon.coupon.messaging.CouponIssueProducer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic couponIssueTopic() {
        return TopicBuilder.name(CouponIssueProducer.TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
