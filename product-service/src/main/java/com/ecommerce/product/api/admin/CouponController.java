package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.CouponService;
import com.ecommerce.product.domain.Coupon;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
@Slf4j
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    public ApiResponse<List<Coupon>> getAllCoupons(Authentication authentication) {
        boolean isAdmin = authentication != null
                && authentication.getAuthorities().stream().anyMatch(authority ->
                "Admin".equals(authority.getAuthority()) || "Staff".equals(authority.getAuthority()));

        List<Coupon> coupons = isAdmin
                ? couponService.getAllCoupons()
                : couponService.getAvailableCoupons(authentication != null ? authentication.getName() : null);
        return ApiResponse.ok(coupons, (long) coupons.size());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<Coupon> getCouponById(@PathVariable UUID id) {
        return ApiResponse.ok(couponService.getCouponById(id), 1L);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<Coupon> addCoupon(@RequestBody Coupon coupon) {
        log.info("Yeni kupon ekleniyor: {}", coupon.getCouponCode());
        return ApiResponse.ok(couponService.createCoupon(coupon), 1L);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<Coupon> updateCoupon(@PathVariable UUID id, @RequestBody Coupon coupon) {
        log.info("Kupon güncelleniyor: ID {}", id);
        return ApiResponse.ok(couponService.updateCoupon(id, coupon), 1L);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<String> deleteCoupon(@PathVariable UUID id) {
        log.info("Kupon siliniyor: ID {}", id);
        couponService.deleteCoupon(id);
        return ApiResponse.ok("Kupon başarıyla silindi.", 1L);
    }
}
