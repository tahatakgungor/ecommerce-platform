package com.ecommerce.product.dto.auth;

// Field isimlerinin Frontend'den gelenle (email, password) aynı olduğundan emin ol
public record LoginRequest(String email, String password) {}