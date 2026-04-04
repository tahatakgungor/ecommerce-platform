package com.ecommerce.product.dto.review;

import lombok.Data;

import java.util.List;

@Data
public class ReviewUpdateRequest {
    private Integer rating;
    private String commentTitle;
    private String commentBody;
    private List<String> mediaUrls;
}
