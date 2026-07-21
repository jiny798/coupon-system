package dev.kenzi.coupon.coupon.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kenzi.coupon.auth.jwt.JwtTokenProvider;
import dev.kenzi.coupon.coupon.dto.CouponCreateRequest;
import dev.kenzi.coupon.coupon.dto.IssuedCouponResponse;
import dev.kenzi.coupon.coupon.exception.CouponSoldOutException;
import dev.kenzi.coupon.coupon.service.CouponService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CouponController.class)
class CouponControllerTest {

    private static final String AUTH = "Bearer valid-token";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    CouponService couponService;

    @MockBean
    JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        given(jwtTokenProvider.getUserId("valid-token")).willReturn(1L);
    }

    @Test
    @DisplayName("쿠폰 생성 성공 시 201과 생성된 id를 응답한다")
    void create_returns_201() throws Exception {
        given(couponService.create(any())).willReturn(1L);

        mockMvc.perform(post("/api/coupons")
                        .header(HttpHeaders.AUTHORIZATION, AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CouponCreateRequest(
                                "선착순 쿠폰", 100,
                                LocalDateTime.of(2026, 1, 1, 0, 0),
                                LocalDateTime.of(2026, 12, 31, 23, 59)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("쿠폰 이름이 비어 있으면 400을 응답한다")
    void create_rejects_blank_name() throws Exception {
        mockMvc.perform(post("/api/coupons")
                        .header(HttpHeaders.AUTHORIZATION, AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CouponCreateRequest(
                                "", 100,
                                LocalDateTime.of(2026, 1, 1, 0, 0),
                                LocalDateTime.of(2026, 12, 31, 23, 59)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("토큰 없이 쿠폰을 생성하면 401을 응답한다")
    void create_returns_401_without_token() throws Exception {
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CouponCreateRequest(
                                "선착순 쿠폰", 100,
                                LocalDateTime.of(2026, 1, 1, 0, 0),
                                LocalDateTime.of(2026, 12, 31, 23, 59)))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("쿠폰 발급 성공 시 201과 발급 내역 id를 응답한다")
    void issue_returns_201() throws Exception {
        given(couponService.issue(1L, 1L)).willReturn(100L);

        mockMvc.perform(post("/api/coupons/1/issue")
                        .header(HttpHeaders.AUTHORIZATION, AUTH))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.issuedCouponId").value(100));
    }

    @Test
    @DisplayName("재고가 소진된 쿠폰을 발급하면 409를 응답한다")
    void issue_returns_409_when_sold_out() throws Exception {
        given(couponService.issue(1L, 1L)).willThrow(new CouponSoldOutException());

        mockMvc.perform(post("/api/coupons/1/issue")
                        .header(HttpHeaders.AUTHORIZATION, AUTH))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("내 쿠폰 목록을 응답한다")
    void my_coupons_returns_list() throws Exception {
        given(couponService.getMyCoupons(1L)).willReturn(List.of(
                new IssuedCouponResponse(100L, 1L, "선착순 쿠폰",
                        LocalDateTime.of(2026, 7, 21, 12, 0), null, false)
        ));

        mockMvc.perform(get("/api/coupons/my")
                        .header(HttpHeaders.AUTHORIZATION, AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].couponName").value("선착순 쿠폰"))
                .andExpect(jsonPath("$[0].used").value(false));
    }

    @Test
    @DisplayName("쿠폰 사용 성공 시 204를 응답한다")
    void use_returns_204() throws Exception {
        mockMvc.perform(post("/api/coupons/issued/100/use")
                        .header(HttpHeaders.AUTHORIZATION, AUTH))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("남의 쿠폰을 사용하면 403을 응답한다")
    void use_returns_403_when_not_owner() throws Exception {
        willThrow(new dev.kenzi.coupon.coupon.exception.NotCouponOwnerException())
                .given(couponService).use(100L, 1L);

        mockMvc.perform(post("/api/coupons/issued/100/use")
                        .header(HttpHeaders.AUTHORIZATION, AUTH))
                .andExpect(status().isForbidden());
    }
}
