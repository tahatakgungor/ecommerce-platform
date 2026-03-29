package com.ecommerce.product.api.client;

import com.ecommerce.product.application.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products") // Harri bunu bekliyor
@RequiredArgsConstructor
public class ProductClientController {

    private final ProductService productService;
/*
    // GET /api/products/show
    @GetMapping("/show")
    public List<ProductResponse> getShowingProducts() {
        return productService.findAll();
    }

    // GET /api/products/{id}
    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable UUID id) {
        return productService.findById(id);
    }

 */
}