package com.ecommerce.product.application;

import com.ecommerce.product.domain.SiteSettings;
import com.ecommerce.product.repository.SiteSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteSettingsServiceTest {

    @Mock
    private SiteSettingsRepository siteSettingsRepository;

    @InjectMocks
    private SiteSettingsService siteSettingsService;

    @Test
    void getPublicSettings_shouldIncludeShippingFields() {
        SiteSettings settings = new SiteSettings();
        settings.setSingletonKey("default");
        settings.setFreeShippingThreshold(400);
        settings.setDefaultShippingFee(59.9);
        settings.setAnnouncementActive(true);

        when(siteSettingsRepository.findBySingletonKey("default")).thenReturn(Optional.of(settings));

        Map<String, Object> result = siteSettingsService.getPublicSettings();

        assertEquals(400, result.get("freeShippingThreshold"));
        assertEquals(59.9, result.get("defaultShippingFee"));
        assertEquals(true, result.get("announcementActive"));
    }

    @Test
    void calculateShippingCost_shouldBeZeroAboveThreshold() {
        SiteSettings settings = new SiteSettings();
        settings.setSingletonKey("default");
        settings.setFreeShippingThreshold(400);
        settings.setDefaultShippingFee(49.9);
        when(siteSettingsRepository.findBySingletonKey("default")).thenReturn(Optional.of(settings));

        assertEquals(0.0, siteSettingsService.calculateShippingCost(500.0));
    }

    @Test
    void calculateShippingCost_shouldReturnDefaultFeeBelowThreshold() {
        SiteSettings settings = new SiteSettings();
        settings.setSingletonKey("default");
        settings.setFreeShippingThreshold(400);
        settings.setDefaultShippingFee(49.9);
        when(siteSettingsRepository.findBySingletonKey("default")).thenReturn(Optional.of(settings));

        assertEquals(49.9, siteSettingsService.calculateShippingCost(399.99));
    }
}
