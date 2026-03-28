package com.ecommerce.product.api;

import com.ecommerce.product.domain.Brand;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/brand")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "https://ecommerce-frontend-xryc.vercel.app"})
public class BrandController {

    @GetMapping("/all")
    public ApiResponse<List<Brand>> getAllBrands() {
        List<Brand> brands = new ArrayList<>();

        // Veritabanı boşsa bile Frontend çökmesin diye 1 tane dummy ekliyoruz
        Brand defaultBrand = new Brand();
        defaultBrand.setId(UUID.randomUUID());
        defaultBrand.setName("Apple"); // Harri 'name' alanını okur
        brands.add(defaultBrand);

        return new ApiResponse<>(true, brands, (long) brands.size());
    }
}