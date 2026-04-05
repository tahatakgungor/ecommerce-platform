package com.ecommerce.product.application;

import com.ecommerce.product.domain.HeroBanner;
import com.ecommerce.product.repository.HeroBannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HeroBannerService {

    private final HeroBannerRepository heroBannerRepository;

    @Transactional(readOnly = true)
    public List<HeroBanner> getActiveBanners() {
        return heroBannerRepository.findByActiveTrueOrderBySortOrderAscUpdatedAtDesc();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public List<HeroBanner> getAllBannersForAdmin() {
        return heroBannerRepository.findAllByOrderBySortOrderAscUpdatedAtDesc();
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public HeroBanner createBanner(HeroBanner request) {
        validateBanner(request);
        HeroBanner banner = new HeroBanner();
        applyFields(banner, request);
        return heroBannerRepository.save(banner);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public HeroBanner updateBanner(UUID id, HeroBanner request) {
        HeroBanner banner = heroBannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner bulunamadı."));
        validateBanner(request);
        applyFields(banner, request);
        return heroBannerRepository.save(banner);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public HeroBanner toggleBanner(UUID id, Boolean active) {
        HeroBanner banner = heroBannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner bulunamadı."));
        banner.setActive(active != null ? active : !banner.isActive());
        return heroBannerRepository.save(banner);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public void deleteBanner(UUID id) {
        HeroBanner banner = heroBannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner bulunamadı."));
        heroBannerRepository.delete(banner);
    }

    private void applyFields(HeroBanner target, HeroBanner source) {
        target.setTitle(trimOrEmpty(source.getTitle()));
        target.setSubtitle(trimOrNull(source.getSubtitle()));
        target.setCtaLabel(trimOrNull(source.getCtaLabel()));
        target.setCtaLink(trimOrNull(source.getCtaLink()));
        target.setImageUrl(trimOrEmpty(source.getImageUrl()));
        target.setImageAlt(trimOrNull(source.getImageAlt()));
        target.setActive(source.isActive());
        target.setOpenInNewTab(source.isOpenInNewTab());
        target.setSortOrder(source.getSortOrder() != null ? source.getSortOrder() : 0);
    }

    private void validateBanner(HeroBanner request) {
        if (request == null) throw new RuntimeException("Geçersiz banner verisi.");
        String title = trimOrEmpty(request.getTitle());
        String imageUrl = trimOrEmpty(request.getImageUrl());
        if (title.isBlank()) {
            throw new RuntimeException("Banner başlığı zorunludur.");
        }
        if (imageUrl.isBlank()) {
            throw new RuntimeException("Banner görseli zorunludur.");
        }
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimOrNull(String value) {
        String trimmed = trimOrEmpty(value);
        return trimmed.isBlank() ? null : trimmed;
    }
}
