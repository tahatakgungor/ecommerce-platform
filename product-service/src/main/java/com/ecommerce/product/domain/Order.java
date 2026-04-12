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
    
    @Column(columnDefinition = "TEXT")
    private String orderNote;

    // pending | processing | shipped | delivered | cancelled
    private String status;

    // Kargo Takip Bilgileri
    private String shippingCarrier;
    private String trackingNumber;
    private LocalDateTime shippedAt;

    // müşterinin UUID string hali (login'li kullanıcılar için)
    private String userId;

    // Misafir alışverişi için alanlar
    private String guestEmail;
    private String guestName;
    private String guestPhone;
    private Boolean isGuest = false;

    // INV-xxxxxx
    private String invoice;

    // Denormalized return status for fast lookup (mirrors latest OrderReturn.status)
    private String returnStatus;

    // sepet ürünleri JSON olarak saklanıyor
    @Column(columnDefinition = "TEXT")
    private String cart;

    private Double subTotal;
    private Double shippingCost;
    private Double discount;
    private Double totalAmount;
    private String couponCode;
    private String couponTitle;

    // iyzico ödeme alanları
    @Column(unique = true)
    private String iyzicoToken;

    private String iyzicoConversationId;
    private String iyzicoPaymentId;

    @Column(columnDefinition = "TEXT")
    private String iyzicoPaymentDetail;

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
