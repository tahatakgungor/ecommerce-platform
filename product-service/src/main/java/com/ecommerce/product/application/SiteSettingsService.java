package com.ecommerce.product.application;

import com.ecommerce.product.domain.SiteSettings;
import com.ecommerce.product.repository.SiteSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SiteSettingsService {

    private static final String DEFAULT_KEY = "default";
    private final SiteSettingsRepository siteSettingsRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getPublicSettings() {
        SiteSettings settings = getOrCreate();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("announcementActive", settings.isAnnouncementActive());
        result.put("announcementTextTr", settings.getAnnouncementTextTr());
        result.put("announcementTextEn", settings.getAnnouncementTextEn());
        result.put("announcementLink", settings.getAnnouncementLink());
        result.put("announcementSpeed", settings.getAnnouncementSpeed());
        result.put("whatsappNumber", settings.getWhatsappNumber());
        result.put("whatsappLabel", settings.getWhatsappLabel());
        result.put("supportEmail", settings.getSupportEmail());
        result.put("supportPhone", settings.getSupportPhone());
        result.put("returnWindowDays", settings.getReturnWindowDays());
        return result;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public SiteSettings getAdminSettings() {
        return getOrCreate();
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public SiteSettings update(Map<String, Object> body) {
        SiteSettings settings = getOrCreate();

        if (body.containsKey("announcementActive")) {
            settings.setAnnouncementActive(Boolean.parseBoolean(String.valueOf(body.get("announcementActive"))));
        }
        if (body.containsKey("announcementTextTr")) {
            settings.setAnnouncementTextTr(trimToNull(body.get("announcementTextTr")));
        }
        if (body.containsKey("announcementTextEn")) {
            settings.setAnnouncementTextEn(trimToNull(body.get("announcementTextEn")));
        }
        if (body.containsKey("announcementLink")) {
            settings.setAnnouncementLink(trimToNull(body.get("announcementLink")));
        }
        if (body.containsKey("announcementSpeed")) {
            settings.setAnnouncementSpeed(parsePositiveInt(body.get("announcementSpeed"), 40));
        }
        if (body.containsKey("whatsappNumber")) {
            settings.setWhatsappNumber(trimToNull(body.get("whatsappNumber")));
        }
        if (body.containsKey("whatsappLabel")) {
            settings.setWhatsappLabel(trimToNull(body.get("whatsappLabel")));
        }
        if (body.containsKey("supportEmail")) {
            settings.setSupportEmail(trimToNull(body.get("supportEmail")));
        }
        if (body.containsKey("supportPhone")) {
            settings.setSupportPhone(trimToNull(body.get("supportPhone")));
        }
        if (body.containsKey("returnWindowDays")) {
            settings.setReturnWindowDays(parsePositiveInt(body.get("returnWindowDays"), 14));
        }

        return siteSettingsRepository.save(settings);
    }

    @Transactional(readOnly = true)
    public int getReturnWindowDays() {
        return getOrCreate().getReturnWindowDays();
    }

    private SiteSettings getOrCreate() {
        return siteSettingsRepository.findBySingletonKey(DEFAULT_KEY)
                .orElseGet(() -> {
                    SiteSettings created = new SiteSettings();
                    created.setSingletonKey(DEFAULT_KEY);
                    return siteSettingsRepository.save(created);
                });
    }

    private String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private int parsePositiveInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(String.valueOf(value));
            return parsed > 0 ? parsed : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}
