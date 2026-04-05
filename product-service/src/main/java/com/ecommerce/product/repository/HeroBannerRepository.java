package com.ecommerce.product.repository;

import com.ecommerce.product.domain.HeroBanner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HeroBannerRepository extends JpaRepository<HeroBanner, UUID> {
    List<HeroBanner> findAllByOrderBySortOrderAscUpdatedAtDesc();
    List<HeroBanner> findByActiveTrueOrderBySortOrderAscUpdatedAtDesc();
}
