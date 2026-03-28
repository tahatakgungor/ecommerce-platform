package com.ecommerce.product.dto.auth;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String name;
    // getter setter
}