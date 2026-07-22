package dev.kenzi.coupon.coupon.controller;

import dev.kenzi.coupon.auth.support.LoginUser;
import dev.kenzi.coupon.coupon.dto.CouponCreateRequest;
import dev.kenzi.coupon.coupon.dto.CouponCreateResponse;
import dev.kenzi.coupon.coupon.dto.CouponIssueResponse;
import dev.kenzi.coupon.coupon.dto.IssuedCouponResponse;
import dev.kenzi.coupon.coupon.service.CouponService;
import dev.kenzi.coupon.coupon.service.OptimisticCouponIssueFacade;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final OptimisticCouponIssueFacade couponIssueFacade;

    @PostMapping
    public ResponseEntity<CouponCreateResponse> create(@RequestBody @Valid CouponCreateRequest request) {
        Long id = couponService.create(request);
        return ResponseEntity
                .created(URI.create("/api/coupons/" + id))
                .body(new CouponCreateResponse(id));
    }

    @PostMapping("/{couponId}/issue")
    public ResponseEntity<CouponIssueResponse> issue(
            @PathVariable Long couponId,
            @LoginUser Long userId
    ) {
        Long issuedCouponId = couponIssueFacade.issue(couponId, userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new CouponIssueResponse(issuedCouponId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<IssuedCouponResponse>> myCoupons(@LoginUser Long userId) {
        return ResponseEntity.ok(couponService.getMyCoupons(userId));
    }

    @PostMapping("/issued/{issuedCouponId}/use")
    public ResponseEntity<Void> use(
            @PathVariable Long issuedCouponId,
            @LoginUser Long userId
    ) {
        couponService.use(issuedCouponId, userId);
        return ResponseEntity.noContent().build();
    }
}
