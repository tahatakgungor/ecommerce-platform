package com.ecommerce.product.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "site_settings")
@Data
public class SiteSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String singletonKey = "default";

    @Column(length = 255)
    private String announcementTextTr;

    @Column(length = 255)
    private String announcementTextEn;

    @Column(length = 500)
    private String announcementLink;

    @Column(nullable = false)
    private boolean announcementActive = false;

    @Column(nullable = false)
    private int announcementSpeed = 40;

    @Column(length = 40)
    private String whatsappNumber;

    @Column(length = 120)
    private String whatsappLabel;

    @Column(length = 320)
    private String supportEmail;

    @Column(length = 40)
    private String supportPhone;

    @Column(nullable = false)
    private int returnWindowDays = 14;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        this.createdAt = now;
        this.updatedAt = now;
        if (this.singletonKey == null || this.singletonKey.isBlank()) {
            this.singletonKey = "default";
        }
        if (this.announcementSpeed <= 0) {
            this.announcementSpeed = 40;
        }
        if (this.returnWindowDays <= 0) {
            this.returnWindowDays = 14;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        if (this.announcementSpeed <= 0) {
            this.announcementSpeed = 40;
        }
        if (this.returnWindowDays <= 0) {
            this.returnWindowDays = 14;
        }
    }
}
