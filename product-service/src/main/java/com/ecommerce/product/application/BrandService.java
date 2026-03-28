package com.ecommerce.product.application;

import com.ecommerce.product.domain.Brand;
import com.ecommerce.product.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    public Brand getBrandById(UUID id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marka bulunamadı!"));
    }

    @Transactional
    @PreAuthorize("hasAuthority('Admin')")
    public Brand createBrand(Brand brand) {
        if (brandRepository.findByName(brand.getName()).isPresent()) {
            throw new RuntimeException("Bu marka zaten kayıtlı!");
        }
        return brandRepository.save(brand);
    }

    @Transactional
    @PreAuthorize("hasAuthority('Admin')")
    public Brand updateBrand(UUID id, Brand details) {
        Brand brand = getBrandById(id);
        brand.setName(details.getName());
        brand.setEmail(details.getEmail());
        brand.setWebsite(details.getWebsite());
        brand.setLocation(details.getLocation());
        brand.setDescription(details.getDescription());
        brand.setLogo(details.getLogo());
        brand.setStatus(details.getStatus());
        return brandRepository.save(brand);
    }

    @Transactional
    @PreAuthorize("hasAuthority('Admin')")
    public void deleteBrand(UUID id) {
        if (!brandRepository.existsById(id)) {
            throw new RuntimeException("Silinecek marka bulunamadı.");
        }
        brandRepository.deleteById(id);
    }
}