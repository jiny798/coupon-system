package dev.kenzi.coupon.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import dev.kenzi.coupon.auth.dto.LoginRequest;
import dev.kenzi.coupon.auth.dto.LoginResponse;
import dev.kenzi.coupon.auth.exception.InvalidLoginException;
import dev.kenzi.coupon.auth.jwt.JwtTokenProvider;
import dev.kenzi.coupon.user.domain.User;
import dev.kenzi.coupon.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider("test-secret-key-for-jwt-provider-32-bytes-or-more", 3_600_000L);

    AuthService authService;

    User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider);
        user = User.builder()
                .email("kenzi@test.com")
                .password(passwordEncoder.encode("password123"))
                .name("켄지")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    @DisplayName("로그인에 성공하면 userId가 담긴 액세스 토큰을 발급한다")
    void login_issues_token() {
        given(userRepository.findByEmail("kenzi@test.com")).willReturn(Optional.of(user));

        LoginResponse response = authService.login(new LoginRequest("kenzi@test.com", "password123"));

        assertThat(jwtTokenProvider.getUserId(response.accessToken())).isEqualTo(1L);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 예외가 발생한다")
    void login_rejects_wrong_password() {
        given(userRepository.findByEmail("kenzi@test.com")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("kenzi@test.com", "wrong-password")))
                .isInstanceOf(InvalidLoginException.class);
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 예외가 발생한다")
    void login_rejects_unknown_email() {
        given(userRepository.findByEmail("nobody@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@test.com", "password123")))
                .isInstanceOf(InvalidLoginException.class);
    }
}
