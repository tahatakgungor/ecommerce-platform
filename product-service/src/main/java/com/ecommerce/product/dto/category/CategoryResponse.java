package com.ecommerce.product.dto.category;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    @JsonProperty("_id")
    private UUID id;

    @JsonProperty("parent") // Harri ana kategori ismini burada bekler
    private String name;

    private String description;
    private String image;

    @Builder.Default
    private List<String> children = new ArrayList<>();

    @Builder.Default
    private Integer productCount = 0; // Harri'nin yanındaki (12) gibi rakamlar için
}