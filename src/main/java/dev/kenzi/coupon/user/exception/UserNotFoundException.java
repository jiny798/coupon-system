package dev.kenzi.coupon.user.exception;

import dev.kenzi.coupon.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(Long userId) {
        super(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다: id=" + userId);
    }
}
