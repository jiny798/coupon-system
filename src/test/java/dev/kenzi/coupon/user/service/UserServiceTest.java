package dev.kenzi.coupon.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dev.kenzi.coupon.user.domain.User;
import dev.kenzi.coupon.user.dto.UserSignupRequest;
import dev.kenzi.coupon.user.exception.DuplicateEmailException;
import dev.kenzi.coupon.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 순수 단위 테스트: 스프링 컨텍스트/DB 없이 서비스 로직만 검증한다. (수 초 안에 끝남)
 * Repository는 Mockito로 가짜(mock)를 만들고, PasswordEncoder는 진짜를 쓴다.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("회원가입에 성공하면 비밀번호가 원문이 아닌 BCrypt 해시로 저장된다")
    void signup_hashes_password() {
        // given
        UserSignupRequest request = new UserSignupRequest("kenzi@test.com", "password123", "켄지");
        given(userRepository.existsByEmail("kenzi@test.com")).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L); // DB가 채워줄 id를 흉내
            return user;
        });

        // when
        Long id = userService.signup(request);

        // then
        assertThat(id).isEqualTo(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getPassword()).isNotEqualTo("password123");          // 원문이 아니고
        assertThat(passwordEncoder.matches("password123", saved.getPassword())) // 해시가 원문과 매칭된다
                .isTrue();
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 예외가 발생하고 저장하지 않는다")
    void signup_rejects_duplicate_email() {
        // given
        UserSignupRequest request = new UserSignupRequest("kenzi@test.com", "password123", "켄지");
        given(userRepository.existsByEmail("kenzi@test.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(DuplicateEmailException.class);
        verify(userRepository, never()).save(any());
    }
}
