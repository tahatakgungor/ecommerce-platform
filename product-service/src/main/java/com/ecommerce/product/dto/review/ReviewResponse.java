package com.ecommerce.product.dto.review;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ReviewResponse {
    @JsonProperty("reviewId")
    private UUID reviewId;

    @JsonProperty("productId")
    private UUID productId;

    @JsonProperty("userId")
    private UUID userId;

    private String userName;
    private Integer rating;
    private String commentTitle;
    private String commentBody;
    private String status;
    private Boolean verifiedPurchase;
    private List<String> mediaUrls;
    private Long helpfulCount;
    private Long notHelpfulCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
