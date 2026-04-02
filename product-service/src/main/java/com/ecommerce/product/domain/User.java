package com.ecommerce.product.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String name;
    private String role;
    private String phone;
    private String address;
    private String city;
    private String country;
    private String zipCode;
    private String passwordResetToken;
    private String emailVerificationToken;

    // Mevcut kullanıcılar için DB default false — NULL'dan korunmak için columnDefinition
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean emailVerified = false;
}