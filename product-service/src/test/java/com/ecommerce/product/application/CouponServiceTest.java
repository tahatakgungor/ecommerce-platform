package com.ecommerce.product.application;

import com.ecommerce.product.domain.Coupon;
import com.ecommerce.product.domain.User;
import com.ecommerce.product.repository.CouponRepository;
import com.ecommerce.product.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CouponService couponService;

    @Test
    void createCoupon_userScope_shouldBindAssignedUser() {
        Coupon coupon = new Coupon();
        coupon.setTitle("VIP");
        coupon.setCouponCode("vip10");
        coupon.setProductType("Gıda Takviyesi");
        coupon.setEndTime("2099-12-31T23:59:59+03:00");
        coupon.setDiscountPercentage(10);
        coupon.setMinimumAmount(100);
        coupon.setScope("USER");
        coupon.setAssignedUserEmail("Customer@Mail.com");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("customer@mail.com");

        when(couponRepository.findByCouponCodeIgnoreCase("VIP10")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("customer@mail.com")).thenReturn(Optional.of(user));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Coupon saved = couponService.createCoupon(coupon);

        assertEquals("VIP10", saved.getCouponCode());
        assertEquals("USER", saved.getScope());
        assertEquals("customer@mail.com", saved.getAssignedUserEmail());
        assertEquals(user.getId().toString(), saved.getAssignedUserId());
    }

    @Test
    void createCoupon_publicScope_shouldClearAssignedUser() {
        Coupon coupon = new Coupon();
        coupon.setTitle("Genel Kampanya");
        coupon.setCouponCode("public15");
        coupon.setProductType("Detoks");
        coupon.setEndTime("2099-12-31T23:59:59+03:00");
        coupon.setDiscountPercentage(15);
        coupon.setMinimumAmount(0);
        coupon.setScope("PUBLIC");
        coupon.setAssignedUserEmail("someone@mail.com");
        coupon.setAssignedUserId("legacy-id");

        when(couponRepository.findByCouponCodeIgnoreCase("PUBLIC15")).thenReturn(Optional.empty());
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Coupon saved = couponService.createCoupon(coupon);

        assertEquals("PUBLIC", saved.getScope());
        assertNull(saved.getAssignedUserEmail());
        assertNull(saved.getAssignedUserId());
    }

    @Test
    void createCoupon_userScopeWithoutEmail_shouldThrow() {
        Coupon coupon = new Coupon();
        coupon.setTitle("Özel");
        coupon.setCouponCode("special");
        coupon.setProductType("Detoks");
        coupon.setEndTime("2099-12-31T23:59:59+03:00");
        coupon.setDiscountPercentage(20);
        coupon.setMinimumAmount(50);
        coupon.setScope("USER");

        when(couponRepository.findByCouponCodeIgnoreCase("SPECIAL")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> couponService.createCoupon(coupon));
        assertEquals("Kullanıcıya özel kupon için müşteri e-postası zorunludur.", ex.getMessage());
    }

    @Test
    void updateCoupon_scopeChangedToPublic_shouldClearAssignedFields() {
        UUID id = UUID.randomUUID();

        Coupon existing = new Coupon();
        existing.setId(id);
        existing.setTitle("Eski Kupon");
        existing.setCouponCode("OLD10");
        existing.setProductType("Detoks");
        existing.setEndTime("2099-12-31T23:59:59+03:00");
        existing.setDiscountPercentage(10);
        existing.setMinimumAmount(0);
        existing.setScope("USER");
        existing.setAssignedUserEmail("old@mail.com");
        existing.setAssignedUserId("old-id");

        Coupon patch = new Coupon();
        patch.setScope("PUBLIC");

        when(couponRepository.findById(id)).thenReturn(Optional.of(existing));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        couponService.updateCoupon(id, patch);

        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(couponRepository).save(captor.capture());
        Coupon saved = captor.getValue();
        assertEquals("PUBLIC", saved.getScope());
        assertNull(saved.getAssignedUserEmail());
        assertNull(saved.getAssignedUserId());
    }
}
