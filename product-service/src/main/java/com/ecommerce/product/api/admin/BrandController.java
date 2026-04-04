package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.BrandService;
import com.ecommerce.product.domain.Brand;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/brand")
@RequiredArgsConstructor
@Slf4j
public class BrandController {

    private final BrandService brandService;

    @GetMapping("/all")
    public ApiResponse<List<Brand>> getAllBrands() {
        List<Brand> brands = brandService.getAllBrands();
        return ApiResponse.ok(brands, (long) brands.size());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<Brand> getBrandById(@PathVariable UUID id) {
        return ApiResponse.ok(brandService.getBrandById(id), 1L);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<Brand> addBrand(@RequestBody Brand brand) {
        return ApiResponse.ok(brandService.createBrand(brand), 1L);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<Brand> updateBrand(@PathVariable UUID id, @RequestBody Brand brand) {
        return ApiResponse.ok(brandService.updateBrand(id, brand), 1L);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<String> deleteBrand(@PathVariable UUID id) {
        log.info("Admin marka siliyor: ID {}", id); // @Slf4j eklemeyi unutma
        brandService.deleteBrand(id);
        return ApiResponse.ok("Marka başarıyla silindi.", 1L);
    }
}
