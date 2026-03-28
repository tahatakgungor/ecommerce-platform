package com.ecommerce.product.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class ProductRequest {
    @JsonProperty("title")
    private String name;

    private String description;
    private Double price;

    @JsonProperty("quantity")
    private Integer stockQuantity;

    private String sku;
    private Object image; // Obje veya String gelebilir
    private String parent;
    private String children;
    private List<String> tags;
    private List<String> colors;
}