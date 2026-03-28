package com.ecommerce.product.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonProperty("_id")
    private UUID id;

    @JsonProperty("title")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Harri Listeleme (GET /all) sayfasında bu alanı bekliyor
    @JsonProperty("price")
    @Column(name = "price")
    private Double price;

    // Harri Grid/Detay sayfasında (toFixed(2) hatası veren yer) bunu bekliyor
    @JsonProperty("originalPrice")
    @Column(name = "original_price")
    private Double originalPrice;

    @JsonProperty("quantity")
    private Integer stockQuantity;

    private String sku;
    private String image;
    private String status = "Active";

    @JsonProperty("parent")
    private String parentCategory;

    @JsonProperty("children")
    private String childCategory;

    @JsonProperty("category")
    private String categoryName;

    @JsonProperty("brand")
    private String brandName;

    @ElementCollection
    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();

    @ElementCollection
    @JsonProperty("colors")
    private List<String> colors = new ArrayList<>();
}