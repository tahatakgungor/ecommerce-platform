package com.ecommerce.product.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CustomerUserDto(
        @JsonProperty("_id") String id,
        String name,
        String email,
        String role,
        String phone,
        String address,
        String city,
        String country,
        String zipCode
) {}
