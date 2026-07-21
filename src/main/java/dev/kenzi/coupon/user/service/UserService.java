package dev.kenzi.coupon.user.service;

import dev.kenzi.coupon.user.domain.User;
import dev.kenzi.coupon.user.dto.UserResponse;
import dev.kenzi.coupon.user.dto.UserSignupRequest;
import dev.kenzi.coupon.user.exception.DuplicateEmailException;
import dev.kenzi.coupon.user.exception.UserNotFoundException;
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
            throw new DuplicateEmailException(request.email());
        }
    }

    @Transactional(readOnly = true)
    public UserResponse findMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return new UserResponse(user.getId(), user.getEmail(), user.getName());
    }
}
