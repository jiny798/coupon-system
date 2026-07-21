package dev.kenzi.coupon.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kenzi.coupon.user.dto.UserSignupRequest;
import dev.kenzi.coupon.user.exception.DuplicateEmailException;
import dev.kenzi.coupon.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 웹 계층 슬라이스 테스트: 컨트롤러 + 검증 + 예외 핸들러만 띄운다. (DB 불필요)
 * "요청/응답의 모양"을 검증하는 테스트 — 상태코드, JSON 필드, 헤더.
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    UserService userService;

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
}
