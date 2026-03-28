package com.ecommerce.product.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class ProductResponse {
    @JsonProperty("_id")
    private UUID id;

    @JsonProperty("title")
    private String name;

    private String description;

    // --- KRİTİK ALANLAR ---
    // Listeleme tablosu sadece 'price' bekliyor!
    @JsonProperty("price")
    private Double price;

    // Eğer tablo eski fiyatı da (üstü çizili) gösteriyorsa, oraya originalPrice'ı koymalıyız.
    // Şimdilik tabloda tek bir fiyat sütunu var gibi görünüyor, o yüzden ana fiyatı buraya koyacağız.
    // -----------------------

    @JsonProperty("quantity")
    private Integer stockQuantity;

    private String sku;
    private String image;
    private String status;
    private String parentCategory;
    private String childCategory;
    private List<String> tags;
    private List<String> colors;
}