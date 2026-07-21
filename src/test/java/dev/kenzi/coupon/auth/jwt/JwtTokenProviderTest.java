package dev.kenzi.coupon.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kenzi.coupon.auth.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-for-jwt-provider-32-bytes-or-more";

    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, 3_600_000L);

    @Test
    @DisplayName("발급한 토큰에서 userId를 다시 꺼낼 수 있다")
    void create_and_parse() {
        String token = provider.createToken(42L);

        assertThat(provider.getUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("서명이 다른 토큰이면 예외가 발생한다")
    void reject_invalid_signature() {
        JwtTokenProvider other = new JwtTokenProvider("another-secret-key-that-is-also-32-bytes!!", 3_600_000L);
        String token = other.createToken(42L);

        assertThatThrownBy(() -> provider.getUserId(token))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("만료된 토큰이면 예외가 발생한다")
    void reject_expired_token() {
        JwtTokenProvider expired = new JwtTokenProvider(SECRET, -1000L);
        String token = expired.createToken(42L);

        assertThatThrownBy(() -> provider.getUserId(token))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("토큰 형식이 아니면 예외가 발생한다")
    void reject_malformed_token() {
        assertThatThrownBy(() -> provider.getUserId("not-a-jwt"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
