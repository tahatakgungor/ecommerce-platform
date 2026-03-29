package com.ecommerce.product.dto.auth;

public record CustomerLoginResponse(
        String token,
        CustomerUserDto user
) {}
