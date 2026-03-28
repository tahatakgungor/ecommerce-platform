package com.ecommerce.product.api;

import com.ecommerce.product.application.BrandService;
import com.ecommerce.product.domain.Brand;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/brand")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping("/all")
    public ApiResponse<List<Brand>> getAllBrands() {
        List<Brand> brands = brandService.getAllBrands();
        return ApiResponse.ok(brands, (long) brands.size());
    }

    @GetMapping("/{id}")
    public ApiResponse<Brand> getBrandById(@PathVariable UUID id) {
        return ApiResponse.ok(brandService.getBrandById(id), 1L);
    }

    @PostMapping("/add")
    public ApiResponse<Brand> addBrand(@RequestBody Brand brand) {
        return ApiResponse.ok(brandService.createBrand(brand), 1L);
    }

    @PutMapping("/update/{id}")
    public ApiResponse<Brand> updateBrand(@PathVariable UUID id, @RequestBody Brand brand) {
        return ApiResponse.ok(brandService.updateBrand(id, brand), 1L);
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> deleteBrand(@PathVariable UUID id) {
        brandService.deleteBrand(id);
        return ApiResponse.ok("Marka başarıyla silindi.", 1L);
    }
}