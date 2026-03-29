package com.ecommerce.product.dto.brand;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandResponse {
    @JsonProperty("_id")
    private UUID id;
    private String name;
    private String logo;
    private Integer productCount; // Marka yanındaki ürün sayısı (Harri uyumu)
}