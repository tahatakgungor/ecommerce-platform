package com.ecommerce.product.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String address;
    private String contact;
    private String email;
    private String city;
    private String country;
    private String zipCode;
    private String shippingOption;

    // pending | processing | delivered | cancelled
    private String status;

    // müşterinin UUID string hali
    private String userId;

    // INV-xxxxxx
    private String invoice;

    // sepet ürünleri JSON olarak saklanıyor
    @Column(columnDefinition = "TEXT")
    private String cart;

    private Double subTotal;
    private Double shippingCost;
    private Double discount;
    private Double totalAmount;
    private String couponCode;
    private String couponTitle;

    // Stripe PaymentMethod JSON
    @Column(columnDefinition = "TEXT")
    private String cardInfo;

    // Stripe PaymentIntent JSON
    @Column(columnDefinition = "TEXT")
    private String paymentIntent;

    // Sipariş bazlı değerlendirilen ürün ID listesi (JSON array)
    @Column(columnDefinition = "TEXT")
    private String reviewedProducts;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        if (this.invoice == null) {
            this.invoice = "INV-" + System.currentTimeMillis();
        }
        if (this.status == null) {
            this.status = "pending";
        }
    }
}
