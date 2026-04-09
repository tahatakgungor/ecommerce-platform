package com.ecommerce.product.repository;

import com.ecommerce.product.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    Optional<Coupon> findByCouponCode(String couponCode);
    Optional<Coupon> findByCouponCodeIgnoreCase(String couponCode);

    @Modifying
    @Query("""
            UPDATE Coupon c
            SET c.assignedUserEmail = null,
                c.assignedUserId = null
            WHERE lower(c.assignedUserEmail) = lower(:email)
               OR c.assignedUserId = :userId
            """)
    int clearAssignmentsForUser(@Param("email") String email, @Param("userId") String userId);
}
