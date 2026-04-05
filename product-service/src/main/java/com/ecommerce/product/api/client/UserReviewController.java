package com.ecommerce.product.api.client;

import com.ecommerce.product.application.ProductReviewService;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user/reviews")
@RequiredArgsConstructor
public class UserReviewController {

    private final ProductReviewService productReviewService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyReviewOverview(Authentication authentication) {
        Map<String, Object> result = productReviewService.getMyReviewOverview(authentication.getName());
        long total = 0L;
        Object reviewedObj = result.get("reviewed");
        Object pendingObj = result.get("pending");
        if (reviewedObj instanceof java.util.List<?> reviewed) total += reviewed.size();
        if (pendingObj instanceof java.util.List<?> pending) total += pending.size();
        return ResponseEntity.ok(ApiResponse.ok(result, total));
    }
}
