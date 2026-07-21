package dev.kenzi.coupon.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    /**
     * BCrypt 해싱을 담당하는 빈.
     * spring-security-crypto만 의존하므로 Security 필터 체인은 등록되지 않는다.
     * (= 모든 API가 인증 없이 열려 있는 상태. 인증은 Phase 1 후반에 직접 만든다)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
