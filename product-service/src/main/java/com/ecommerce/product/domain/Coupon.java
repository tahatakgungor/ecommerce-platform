package com.ecommerce.product.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String logo;

    @Column(nullable = false, unique = true)
    private String couponCode;

    @Column(nullable = false)
    private String endTime;

    private String startTime;

    @Column(nullable = false)
    private double discountPercentage;

    @Column(nullable = false)
    private double minimumAmount;

    @Column(nullable = false)
    private String productType;

    private String status = "Active";

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @JsonProperty("_id")
    public String getHarriId() {
        return id != null ? id.toString() : null;
    }
}
