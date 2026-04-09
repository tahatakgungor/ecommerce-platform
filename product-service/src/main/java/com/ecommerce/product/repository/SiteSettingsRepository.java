package com.ecommerce.product.repository;

import com.ecommerce.product.domain.SiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SiteSettingsRepository extends JpaRepository<SiteSettings, UUID> {
    Optional<SiteSettings> findBySingletonKey(String singletonKey);
}
