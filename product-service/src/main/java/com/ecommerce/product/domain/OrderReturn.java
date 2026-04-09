package com.ecommerce.product.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "order_returns")
@Data
public class OrderReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    private String userId;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private boolean guestOrder;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String customerNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderReturnStatus status = OrderReturnStatus.REQUESTED;

    @Column(columnDefinition = "TEXT")
    private String statusHistory;

    private String adminNote;

    private String processedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = OrderReturnStatus.REQUESTED;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
    }
}
