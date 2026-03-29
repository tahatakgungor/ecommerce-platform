package com.ecommerce.product.api.client;

import com.ecommerce.product.application.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/brands")
@RequiredArgsConstructor
public class BrandClientController {

    private final BrandService brandService;

    /*
    @GetMapping("/all")
    public List<BrandResponse> getAllPublicBrands() {
        return brandService.getAllBrandsForClient();
    }

     */
}