package dev.kenzi.coupon.user.repository;

import dev.kenzi.coupon.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    /** 메서드 이름 규칙으로 쿼리 자동 생성: select exists(... where email = ?) */
    boolean existsByEmail(String email);
}
