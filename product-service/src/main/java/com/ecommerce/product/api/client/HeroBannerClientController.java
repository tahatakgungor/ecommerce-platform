package com.ecommerce.product.api.client;

import com.ecommerce.product.application.HeroBannerService;
import com.ecommerce.product.domain.HeroBanner;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class HeroBannerClientController {

    private final HeroBannerService heroBannerService;

    @GetMapping("/show")
    public Map<String, Object> getShowingBanners() {
        List<HeroBanner> banners = heroBannerService.getActiveBanners();
        return Map.of("banners", banners);
    }
}
