package com.ecommerce.product.api.client;

import com.ecommerce.product.application.SiteSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site-settings")
@RequiredArgsConstructor
public class SiteSettingsClientController {

    private final SiteSettingsService siteSettingsService;

    @GetMapping
    public ResponseEntity<?> getPublicSettings() {
        return ResponseEntity.ok(siteSettingsService.getPublicSettings());
    }
}
