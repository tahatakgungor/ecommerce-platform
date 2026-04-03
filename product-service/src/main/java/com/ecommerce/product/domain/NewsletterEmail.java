package com.ecommerce.product.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "newsletter_emails")
@Data
public class NewsletterEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private LocalDateTime subscribedAt;

    @PrePersist
    public void prePersist() {
        this.subscribedAt = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
    }
}
