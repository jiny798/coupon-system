package dev.kenzi.coupon.user.service;

import dev.kenzi.coupon.user.domain.User;
import dev.kenzi.coupon.user.dto.UserSignupRequest;
import dev.kenzi.coupon.user.exception.DuplicateEmailException;
import dev.kenzi.coupon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signup(UserSignupRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .build();

        try {
            return userRepository.save(user).getId();
        } catch (DataIntegrityViolationException e) {
            // 2차 방어(최후 방어선): existsByEmail 통과 직후 다른 요청이 먼저 INSERT한 경우.
            // check-then-act 사이의 틈은 애플리케이션 코드로 못 막고, DB 유니크 제약만이 원자적으로 보장한다.
            throw new DuplicateEmailException(request.email());
        }
    }
}
