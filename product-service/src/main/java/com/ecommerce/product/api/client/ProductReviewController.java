package com.ecommerce.product.api.client;

import com.ecommerce.product.application.ProductReviewService;
import com.ecommerce.product.dto.ApiResponse;
import com.ecommerce.product.dto.review.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getApprovedReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "false") boolean withMedia,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Map<String, Object> result = productReviewService.getApprovedReviews(productId, sort, withMedia, page, size);
        Long total = (Long) result.get("totalElements");
        return ResponseEntity.ok(ApiResponse.ok(result, total));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getReviewSummary(@PathVariable UUID productId) {
        ReviewSummaryResponse summary = productReviewService.getReviewSummary(productId);
        return ResponseEntity.ok(ApiResponse.ok(summary, summary.getTotalReviews()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable UUID productId,
            @RequestBody ReviewCreateRequest request,
            Authentication authentication
    ) {
        ReviewResponse response = productReviewService.createReview(productId, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, 1L));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable UUID productId,
            @PathVariable UUID reviewId,
            @RequestBody ReviewUpdateRequest request,
            Authentication authentication
    ) {
        ReviewResponse response = productReviewService.updateOwnReview(productId, reviewId, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, 1L));
    }

    @PostMapping("/{reviewId}/vote")
    public ResponseEntity<ApiResponse<Map<String, Object>>> voteReview(
            @PathVariable UUID productId,
            @PathVariable UUID reviewId,
            @RequestBody ReviewVoteRequest request,
            Authentication authentication
    ) {
        boolean helpful = request.getHelpful() != null && request.getHelpful();
        Map<String, Object> result = productReviewService.voteReview(productId, reviewId, helpful, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(result, 1L));
    }
}
