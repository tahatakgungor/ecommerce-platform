package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.HeroBannerService;
import com.ecommerce.product.domain.HeroBanner;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('Admin','Staff')")
public class HeroBannerAdminController {

    private final HeroBannerService heroBannerService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HeroBanner>>> getAllBanners() {
        List<HeroBanner> banners = heroBannerService.getAllBannersForAdmin();
        return ResponseEntity.ok(ApiResponse.ok(banners, (long) banners.size()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HeroBanner>> createBanner(@RequestBody HeroBanner request) {
        HeroBanner banner = heroBannerService.createBanner(request);
        return ResponseEntity.ok(ApiResponse.ok(banner, 1L));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HeroBanner>> updateBanner(
            @PathVariable UUID id,
            @RequestBody HeroBanner request
    ) {
        HeroBanner banner = heroBannerService.updateBanner(id, request);
        return ResponseEntity.ok(ApiResponse.ok(banner, 1L));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<HeroBanner>> toggleBanner(
            @PathVariable UUID id,
            @RequestParam(required = false) Boolean active
    ) {
        HeroBanner banner = heroBannerService.toggleBanner(id, active);
        return ResponseEntity.ok(ApiResponse.ok(banner, 1L));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteBanner(@PathVariable UUID id) {
        heroBannerService.deleteBanner(id);
        return ResponseEntity.ok(ApiResponse.ok("Banner silindi.", 1L));
    }
}
