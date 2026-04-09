package com.ecommerce.product.application;

import com.ecommerce.product.domain.Coupon;
import com.ecommerce.product.domain.ProductScope;
import com.ecommerce.product.domain.User;
import com.ecommerce.product.repository.CouponRepository;
import com.ecommerce.product.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    public List<Coupon> getAvailableCoupons(String userEmail) {
        return couponRepository.findAll().stream()
                .filter(this::isCouponActive)
                .filter(coupon -> canUseCoupon(coupon, userEmail))
                .toList();
    }

    public Coupon getCouponById(UUID id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kupon bulunamadı!"));
    }

    @Transactional
    public Coupon createCoupon(Coupon coupon) {
        String normalizedCode = normalizeCouponCode(coupon.getCouponCode());
        if (couponRepository.findByCouponCodeIgnoreCase(normalizedCode).isPresent()) {
            throw new RuntimeException("Bu kupon kodu zaten mevcut!");
        }
        coupon.setCouponCode(normalizedCode);
        applyCouponDefaults(coupon);
        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon updateCoupon(UUID id, Coupon details) {
        Coupon coupon = getCouponById(id);
        if (details.getTitle() != null) coupon.setTitle(details.getTitle());
        if (details.getLogo() != null) coupon.setLogo(details.getLogo());
        if (details.getCouponCode() != null) {
            String normalizedCode = normalizeCouponCode(details.getCouponCode());
            couponRepository.findByCouponCodeIgnoreCase(normalizedCode)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new RuntimeException("Bu kupon kodu zaten mevcut!");
                    });
            coupon.setCouponCode(normalizedCode);
        }
        if (details.getEndTime() != null) coupon.setEndTime(details.getEndTime());
        if (details.getStartTime() != null) coupon.setStartTime(details.getStartTime());
        if (details.getDiscountPercentage() > 0) coupon.setDiscountPercentage(details.getDiscountPercentage());
        if (details.getMinimumAmount() >= 0) coupon.setMinimumAmount(details.getMinimumAmount());
        if (details.getProductType() != null) coupon.setProductType(details.getProductType());
        if (details.getProductScope() != null) coupon.setProductScope(details.getProductScope());
        if (details.getStatus() != null) coupon.setStatus(details.getStatus());
        if (details.getScope() != null) coupon.setScope(details.getScope());
        if (details.getAssignedUserEmail() != null || details.getAssignedUserId() != null) {
            coupon.setAssignedUserEmail(details.getAssignedUserEmail());
            coupon.setAssignedUserId(details.getAssignedUserId());
        }
        applyCouponDefaults(coupon);
        return couponRepository.save(coupon);
    }

    @Transactional
    public void deleteCoupon(UUID id) {
        if (!couponRepository.existsById(id)) {
            throw new RuntimeException("Kupon bulunamadı.");
        }
        couponRepository.deleteById(id);
    }

    private void applyCouponDefaults(Coupon coupon) {
        coupon.setTitle(requireText(coupon.getTitle(), "Kupon başlığı zorunludur."));
        coupon.setCouponCode(normalizeCouponCode(coupon.getCouponCode()));
        ProductScope productScope = coupon.getProductScope() == null ? ProductScope.CATEGORY : coupon.getProductScope();
        coupon.setProductScope(productScope);
        if (productScope == ProductScope.CATEGORY) {
            coupon.setProductType(requireText(coupon.getProductType(), "Kategori kapsamlı kupon için kategori zorunludur."));
        } else {
            coupon.setProductType(null);
        }
        coupon.setEndTime(requireText(coupon.getEndTime(), "Kupon bitiş tarihi zorunludur."));
        coupon.setStartTime(normalizeNullable(coupon.getStartTime()));

        if (coupon.getDiscountPercentage() <= 0) {
            throw new RuntimeException("İndirim oranı 0'dan büyük olmalıdır.");
        }
        if (coupon.getMinimumAmount() < 0) {
            throw new RuntimeException("Minimum tutar negatif olamaz.");
        }

        String scope = normalizeScope(coupon.getScope());
        coupon.setScope(scope);

        String status = normalizeNullable(coupon.getStatus());
        coupon.setStatus(status == null ? "Active" : status);

        if ("USER".equals(scope)) {
            String assignedUserEmail = normalizeEmail(coupon.getAssignedUserEmail());
            if (assignedUserEmail == null) {
                throw new RuntimeException("Kullanıcıya özel kupon için müşteri e-postası zorunludur.");
            }
            User assignedUser = userRepository.findByEmail(assignedUserEmail)
                    .orElseThrow(() -> new RuntimeException("Bu e-posta ile kayıtlı müşteri bulunamadı."));
            coupon.setAssignedUserEmail(assignedUser.getEmail().trim().toLowerCase(Locale.ROOT));
            coupon.setAssignedUserId(assignedUser.getId().toString());
        } else {
            coupon.setAssignedUserEmail(null);
            coupon.setAssignedUserId(null);
        }
    }

    private boolean canUseCoupon(Coupon coupon, String userEmail) {
        String scope = normalizeScope(coupon.getScope());
        if ("PUBLIC".equals(scope)) {
            return true;
        }

        String normalizedUserEmail = normalizeEmail(userEmail);
        String assignedUserEmail = normalizeEmail(coupon.getAssignedUserEmail());
        return normalizedUserEmail != null && normalizedUserEmail.equals(assignedUserEmail);
    }

    private boolean isCouponActive(Coupon coupon) {
        if (coupon == null) {
            return false;
        }

        String status = normalizeNullable(coupon.getStatus());
        if (status != null && !"active".equalsIgnoreCase(status)) {
            return false;
        }

        return !isBeforeStart(coupon.getStartTime()) && !isExpired(coupon.getEndTime());
    }

    private boolean isBeforeStart(String rawStartTime) {
        if (rawStartTime == null || rawStartTime.isBlank()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        try {
            return OffsetDateTime.parse(rawStartTime)
                    .atZoneSameInstant(ZoneId.of("Europe/Istanbul"))
                    .toLocalDateTime()
                    .isAfter(now);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(rawStartTime).isAfter(now);
        } catch (DateTimeParseException ignored) {
        }

        return false;
    }

    private boolean isExpired(String rawEndTime) {
        if (rawEndTime == null || rawEndTime.isBlank()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        try {
            return OffsetDateTime.parse(rawEndTime)
                    .atZoneSameInstant(ZoneId.of("Europe/Istanbul"))
                    .toLocalDateTime()
                    .isBefore(now);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(rawEndTime).isBefore(now);
        } catch (DateTimeParseException ignored) {
        }

        return false;
    }

    private String normalizeCouponCode(String couponCode) {
        String normalized = requireText(couponCode, "Kupon kodu zorunludur.");
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeScope(String scope) {
        String normalized = normalizeNullable(scope);
        if (normalized == null) {
            return "PUBLIC";
        }

        String upper = normalized.toUpperCase(Locale.ROOT);
        if (!upper.equals("PUBLIC") && !upper.equals("USER")) {
            throw new RuntimeException("Kupon kapsamı geçersiz.");
        }
        return upper;
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeNullable(email);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String requireText(String value, String message) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new RuntimeException(message);
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
