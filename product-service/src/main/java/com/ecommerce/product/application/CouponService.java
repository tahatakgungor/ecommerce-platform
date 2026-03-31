package com.ecommerce.product.application;

import com.ecommerce.product.domain.Coupon;
import com.ecommerce.product.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    public Coupon getCouponById(UUID id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kupon bulunamadı!"));
    }

    @Transactional
    public Coupon createCoupon(Coupon coupon) {
        if (couponRepository.findByCouponCode(coupon.getCouponCode()).isPresent()) {
            throw new RuntimeException("Bu kupon kodu zaten mevcut!");
        }
        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon updateCoupon(UUID id, Coupon details) {
        Coupon coupon = getCouponById(id);
        if (details.getTitle() != null) coupon.setTitle(details.getTitle());
        if (details.getLogo() != null) coupon.setLogo(details.getLogo());
        if (details.getCouponCode() != null) coupon.setCouponCode(details.getCouponCode());
        if (details.getEndTime() != null) coupon.setEndTime(details.getEndTime());
        if (details.getStartTime() != null) coupon.setStartTime(details.getStartTime());
        if (details.getDiscountPercentage() > 0) coupon.setDiscountPercentage(details.getDiscountPercentage());
        if (details.getMinimumAmount() > 0) coupon.setMinimumAmount(details.getMinimumAmount());
        if (details.getProductType() != null) coupon.setProductType(details.getProductType());
        if (details.getStatus() != null) coupon.setStatus(details.getStatus());
        return couponRepository.save(coupon);
    }

    @Transactional
    public void deleteCoupon(UUID id) {
        if (!couponRepository.existsById(id)) {
            throw new RuntimeException("Kupon bulunamadı.");
        }
        couponRepository.deleteById(id);
    }
}
