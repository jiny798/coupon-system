package dev.kenzi.coupon.global.error;

/**
 * 모든 에러 응답의 공통 형태.
 * 지금은 message 하나지만, 나중에 에러 코드/필드별 상세가 필요해지면 여기에 추가한다.
 */
public record ErrorResponse(String message) {
}
