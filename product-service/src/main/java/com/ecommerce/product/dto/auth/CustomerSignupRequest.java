package com.ecommerce.product.dto.auth;

public record CustomerSignupRequest(
        String name,
        String firstName,
        String lastName,
        String phone,
        String email,
        String password,
        String confirmPassword
) {}
