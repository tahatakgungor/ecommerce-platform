package com.ecommerce.product.dto.auth;

public record CustomerSignupRequest(
        String name,
        String email,
        String password,
        String confirmPassword
) {}
