package dev.kenzi.coupon.user.controller;

import dev.kenzi.coupon.user.dto.UserSignupRequest;
import dev.kenzi.coupon.user.dto.UserSignupResponse;
import dev.kenzi.coupon.user.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<UserSignupResponse> signup(@RequestBody @Valid UserSignupRequest request) {
        Long id = userService.signup(request);

        return ResponseEntity
                .created(URI.create("/api/users/" + id))
                .body(new UserSignupResponse(id));
    }
}
