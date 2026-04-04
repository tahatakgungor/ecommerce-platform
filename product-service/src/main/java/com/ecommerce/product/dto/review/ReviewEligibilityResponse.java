package com.ecommerce.product.dto.review;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewEligibilityResponse {
    private boolean canReview;
    private boolean deliveredPurchase;
    private boolean alreadyReviewed;
    private String reason;
}
