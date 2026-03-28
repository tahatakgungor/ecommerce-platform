package com.ecommerce.product.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
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
    private Double price;
    private Double originalPrice;

    @JsonProperty("quantity")
    private Integer stockQuantity;

    private String sku;
    private String image;
    private String status;

    // --- AKILLICA ÇÖZÜM: Obje Yapısı ---
    @JsonProperty("brand")
    private BrandInfo brand;

    @JsonProperty("category")
    private CategoryInfo category;

    @JsonProperty("parent")
    private String parentCategory;

    @JsonProperty("children")
    private String childCategory;

    private List<String> tags;
    private List<String> colors;

    // Frontend'in .name okuyabilmesi için iç sınıflar
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BrandInfo {
        private String name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryInfo {
        private String name;
    }
}