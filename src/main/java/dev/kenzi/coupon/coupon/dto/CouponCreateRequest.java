package dev.kenzi.coupon.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public record CouponCreateRequest(

        @NotBlank(message = "쿠폰 이름은 필수입니다")
        String name,

        @Positive(message = "총 수량은 1 이상이어야 합니다")
        int totalQuantity,

        @NotNull(message = "유효기간 시작일은 필수입니다")
        LocalDateTime validFrom,

        @NotNull(message = "유효기간 종료일은 필수입니다")
        LocalDateTime validUntil
) {
}
