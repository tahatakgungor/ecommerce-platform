package com.ecommerce.product.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record LoginResponse(
        @JsonProperty("token") String accessToken,
        String email,
        String name,
        String phone,
        String role,
        @JsonProperty("_id") UUID id // Frontend'in beklediği _id formatı
) {}
