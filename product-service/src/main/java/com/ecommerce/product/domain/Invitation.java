package com.ecommerce.product.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invitations")
@Data
public class Invitation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String token; // UUID.randomUUID().toString() ile üretilecek

    @Column(nullable = false)
    private String role; // "ADMIN", "STAFF" vb.

    private LocalDateTime expiryDate; // 24 saat sonra dolsun

    private boolean isUsed = false; // Kullanıldıysa true olacak
}