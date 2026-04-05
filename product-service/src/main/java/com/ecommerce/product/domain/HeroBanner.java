package com.ecommerce.product.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "hero_banners")
@Data
public class HeroBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 180)
    private String subtitle;

    @Column(length = 120)
    private String ctaLabel;

    @Column(length = 500)
    private String ctaLink;

    @Column(nullable = false, length = 800)
    private String imageUrl;

    @Column(length = 255)
    private String imageAlt;

    private boolean active = true;

    private boolean openInNewTab = false;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        this.createdAt = now;
        this.updatedAt = now;
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
    }
}
