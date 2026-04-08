package com.ecommerce.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "activity_logs")
@Data
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String eventType;

    @Column(nullable = false, length = 24)
    private String severity;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(length = 120)
    private String actor;

    @Column(length = 50)
    private String targetType;

    @Column(length = 120)
    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        if (this.severity == null || this.severity.isBlank()) {
            this.severity = "INFO";
        }
    }
}
