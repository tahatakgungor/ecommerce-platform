package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.CouponService;
import com.ecommerce.product.domain.Coupon;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ApiResponse<List<Coupon>> getAllCoupons() {
        List<Coupon> coupons = couponService.getAllCoupons();
        return ApiResponse.ok(coupons, (long) coupons.size());
    }

    @GetMapping("/{id}")
    public ApiResponse<Coupon> getCouponById(@PathVariable UUID id) {
        return ApiResponse.ok(couponService.getCouponById(id), 1L);
    }

    @PostMapping("/add")
    public ApiResponse<Coupon> addCoupon(@RequestBody Coupon coupon) {
        log.info("Yeni kupon ekleniyor: {}", coupon.getCouponCode());
        return ApiResponse.ok(couponService.createCoupon(coupon), 1L);
    }

    @PatchMapping("/{id}")
    public ApiResponse<Coupon> updateCoupon(@PathVariable UUID id, @RequestBody Coupon coupon) {
        log.info("Kupon güncelleniyor: ID {}", id);
        return ApiResponse.ok(couponService.updateCoupon(id, coupon), 1L);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteCoupon(@PathVariable UUID id) {
        log.info("Kupon siliniyor: ID {}", id);
        couponService.deleteCoupon(id);
        return ApiResponse.ok("Kupon başarıyla silindi.", 1L);
    }
}
