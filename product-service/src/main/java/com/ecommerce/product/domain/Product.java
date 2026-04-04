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

    @JsonProperty("price")
    private Double price = 0.0;

    @JsonProperty("originalPrice")
    private Double originalPrice = 0.0;

    @JsonProperty("quantity")
    private Integer stockQuantity = 0;

    private String sku;
    private String image;
    private String status = "Active";

    @JsonProperty("parent")
    private String parentCategory;

    @JsonProperty("children")
    private String childCategory;

    // Mevcut alanların yanına ekle (Örneğin categoryName'in hemen üstüne)
    @JsonProperty("categoryId")
    private UUID categoryId;

    @JsonProperty("category")
    private String categoryName;

    @JsonProperty("brand")
    private String brandName;

    @ElementCollection
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_related_images", joinColumns = @JoinColumn(name = "product_id"))
    @JsonProperty("relatedImages")
    private List<String> relatedImages = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_colors", joinColumns = @JoinColumn(name = "product_id"))
    @JsonProperty("colors")
    private List<String> colors = new ArrayList<>();
}
