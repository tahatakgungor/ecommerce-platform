package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.ProductReviewService;
import com.ecommerce.product.dto.ApiResponse;
import com.ecommerce.product.dto.review.ReviewModerationRequest;
import com.ecommerce.product.dto.review.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('Admin','Staff')")
public class ProductReviewAdminController {

    private final ProductReviewService productReviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReviewsForModeration(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Map<String, Object> result = productReviewService.getModerationReviews(status, page, size);
        Long total = (Long) result.get("totalElements");
        return ResponseEntity.ok(ApiResponse.ok(result, total));
    }

    @PatchMapping("/{reviewId}/status")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReviewStatus(
            @PathVariable UUID reviewId,
            @RequestBody ReviewModerationRequest request
    ) {
        ReviewResponse response = productReviewService.updateReviewStatus(reviewId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.ok(response, 1L));
    }
}
