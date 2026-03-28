package com.ecommerce.product.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    @JsonProperty("_id")
    private UUID id;

    @JsonProperty("title")
    private String name;

    private String description;

    // --- FRONTEND UYUMLULUK ALANLARI ---
    @JsonProperty("price")
    private Double price;

    @JsonProperty("originalPrice")
    private Double originalPrice; // Detay sayfasındaki toFixed hatasını önler

    @JsonProperty("quantity")
    private Integer stockQuantity;

    private String sku;
    private String image;
    private String status;

    @JsonProperty("parent")
    private String parentCategory;

    @JsonProperty("children")
    private String childCategory;

    @JsonProperty("category")
    private String categoryName;

    @JsonProperty("brand")
    private String brandName;

    private List<String> tags;
    private List<String> colors;
}