package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.NewsletterService;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/newsletter")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('Admin','Staff')")
public class NewsletterAdminController {

    private final NewsletterService newsletterService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSubscribers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Map<String, Object> result = newsletterService.getNewsletterSubscribers(page, size);
        Long total = (Long) result.get("totalElements");
        return ResponseEntity.ok(ApiResponse.ok(result, total));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteSubscriber(@PathVariable Long id) {
        newsletterService.deleteSubscriber(id);
        return ResponseEntity.ok(ApiResponse.ok("Abone silindi.", 1L));
    }
}
