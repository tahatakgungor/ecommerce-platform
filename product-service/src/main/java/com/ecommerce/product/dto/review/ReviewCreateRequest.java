package com.ecommerce.product.dto.review;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ReviewCreateRequest {
    private Integer rating;
    private String commentTitle;
    private String commentBody;
    private List<String> mediaUrls;
    private UUID orderId;
}
