package com.ecommerce.product.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(
        name = "product_reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_product_user", columnNames = {"product_id", "user_id"}),
        indexes = {
                @Index(name = "idx_review_product_status_created", columnList = "product_id,status,created_at"),
                @Index(name = "idx_review_status_created", columnList = "status,created_at")
        }
)
@Data
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer rating;

    private String commentTitle;

    @Column(columnDefinition = "TEXT")
    private String commentBody;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(nullable = false)
    private boolean verifiedPurchase;

    @Column(columnDefinition = "TEXT")
    private String mediaUrls;

    @Column(nullable = false)
    private Long helpfulCount = 0L;

    @Column(nullable = false)
    private Long notHelpfulCount = 0L;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = ReviewStatus.PENDING;
        if (this.helpfulCount == null) this.helpfulCount = 0L;
        if (this.notHelpfulCount == null) this.notHelpfulCount = 0L;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
    }
}
