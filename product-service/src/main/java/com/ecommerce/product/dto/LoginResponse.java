package com.ecommerce.product.dto;

// Harri şablonu "accessToken" ismini bekler
public record LoginResponse(String accessToken, String email, String name, String role) {}