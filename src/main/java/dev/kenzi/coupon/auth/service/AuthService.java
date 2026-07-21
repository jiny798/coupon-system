package dev.kenzi.coupon.auth.service;

import dev.kenzi.coupon.auth.dto.LoginRequest;
import dev.kenzi.coupon.auth.dto.LoginResponse;
import dev.kenzi.coupon.auth.exception.InvalidLoginException;
import dev.kenzi.coupon.auth.jwt.JwtTokenProvider;
import dev.kenzi.coupon.user.domain.User;
import dev.kenzi.coupon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidLoginException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidLoginException();
        }

        return new LoginResponse(jwtTokenProvider.createToken(user.getId()));
    }
}
