package com.ecommerce.product.dto.review;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ReviewSummaryResponse {
    private Double averageRating;
    private Long totalReviews;
    private Map<Integer, Long> starCounts;
    private Map<Integer, Double> starPercentages;
}
