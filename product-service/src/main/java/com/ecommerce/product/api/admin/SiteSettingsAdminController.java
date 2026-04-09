package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.SiteSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/site-settings")
@RequiredArgsConstructor
public class SiteSettingsAdminController {

    private final SiteSettingsService siteSettingsService;

    @GetMapping
    public ResponseEntity<?> getSettings() {
        return ResponseEntity.ok(siteSettingsService.getAdminSettings());
    }

    @PutMapping
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(siteSettingsService.update(body));
    }
}
