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
    private Double originalPrice;

    @JsonProperty("quantity")
    private Integer stockQuantity;

    private String sku;

    // Bunlar Frontend'den Obje olarak geliyor olabilir, o yüzden Object yapıyoruz
    private Object image;
    private Object parent;
    private Object children;
    private Object category;
    private Object brand;

    private String status;
    private List<String> tags;
    private List<String> colors;
}
