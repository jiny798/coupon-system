package dev.kenzi.coupon.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kenzi.coupon.auth.exception.UnauthorizedException;
import dev.kenzi.coupon.auth.jwt.JwtTokenProvider;
import dev.kenzi.coupon.user.dto.UserResponse;
import dev.kenzi.coupon.user.dto.UserSignupRequest;
import dev.kenzi.coupon.user.exception.DuplicateEmailException;
import dev.kenzi.coupon.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    UserService userService;

    @MockBean
    JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("회원가입 성공 시 201과 생성된 id, Location 헤더를 응답한다")
    void signup_returns_201() throws Exception {
        given(userService.signup(any())).willReturn(1L);

        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserSignupRequest("kenzi@test.com", "password123", "켄지"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("이메일 형식이 잘못되면 400을 응답한다")
    void signup_rejects_invalid_email() throws Exception {
        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserSignupRequest("not-an-email", "password123", "켄지"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면 400을 응답한다")
    void signup_rejects_short_password() throws Exception {
        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserSignupRequest("kenzi@test.com", "short", "켄지"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("중복 이메일이면 409를 응답한다")
    void signup_returns_409_on_duplicate() throws Exception {
        given(userService.signup(any())).willThrow(new DuplicateEmailException("kenzi@test.com"));

        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserSignupRequest("kenzi@test.com", "password123", "켄지"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("유효한 토큰으로 /me 요청 시 내 정보를 응답한다")
    void me_returns_user_info() throws Exception {
        given(jwtTokenProvider.getUserId("valid-token")).willReturn(1L);
        given(userService.findMe(1L)).willReturn(new UserResponse(1L, "kenzi@test.com", "켄지"));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("kenzi@test.com"))
                .andExpect(jsonPath("$.name").value("켄지"));
    }

    @Test
    @DisplayName("토큰 없이 /me 요청 시 401을 응답한다")
    void me_returns_401_without_token() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 /me 요청 시 401을 응답한다")
    void me_returns_401_with_invalid_token() throws Exception {
        given(jwtTokenProvider.getUserId("bad-token")).willThrow(new UnauthorizedException());

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token"))
                .andExpect(status().isUnauthorized());
    }
}
