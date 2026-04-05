package com.ecommerce.product.api.client;

import com.ecommerce.product.application.ProductService;
import com.ecommerce.product.domain.Product;
import com.ecommerce.product.dto.product.ProductResponse;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductClientController {

    private final ProductRepository productRepository;
    private final ProductService productService;

    // GET /api/products/show
    @GetMapping("/show")
    public Map<String, Object> getShowingProducts() {
        List<ProductResponse> products = productRepository.findByStatus("Active")
                .stream().map(this::toResponse).collect(Collectors.toList());
        return Map.of("products", products);
    }

    // GET /api/products/discount
    @GetMapping("/discount")
    public Map<String, Object> getDiscountProducts() {
        List<ProductResponse> products = productRepository.findDiscountProducts()
                .stream().map(this::toResponse).collect(Collectors.toList());
        return Map.of("products", products);
    }

    // GET /api/products/relatedProduct?tags=tag1,tag2
    @GetMapping("/relatedProduct")
    public Map<String, Object> getRelatedProducts(@RequestParam(required = false) String tags) {
        List<Product> products;
        if (tags != null && !tags.isBlank()) {
            List<String> tagList = Arrays.asList(tags.split(","));
            products = productRepository.findRelatedProducts(tagList);
        } else {
            products = productRepository.findByStatus("Active");
        }
        List<ProductResponse> responses = products.stream().map(this::toResponse).collect(Collectors.toList());
        return Map.of("products", responses);
    }

    // GET /api/products/popular?type=top-rated|best-selling|latest&limit=8
    @GetMapping("/popular")
    public Map<String, Object> getPopularProducts(
            @RequestParam(defaultValue = "top-rated") String type,
            @RequestParam(defaultValue = "8") Integer limit
    ) {
        List<ProductResponse> products = productService.findPopularProducts(type, limit);
        return Map.of("products", products);
    }

    // GET /api/products/{id}  — admin controller'da da var, burada tekrar açmıyoruz

    private ProductResponse toResponse(Product p) {
        double price = p.getPrice() != null ? p.getPrice() : 0.0;
        double originalPrice = p.getOriginalPrice() != null ? p.getOriginalPrice() : price;
        int discount = 0;
        if (originalPrice > 0 && originalPrice > price) {
            discount = (int) Math.round((1 - price / originalPrice) * 100);
        }
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(price)
                .originalPrice(originalPrice)
                .discount(discount)
                .stockQuantity(p.getStockQuantity() != null ? p.getStockQuantity() : 0)
                .sku(p.getSku())
                .image(p.getImage())
                .status(p.getStatus())
                .parentCategory(p.getParentCategory())
                .childCategory(p.getChildCategory())
                .brand(new ProductResponse.BrandInfo(p.getBrandName() != null ? p.getBrandName() : ""))
                .category(new ProductResponse.CategoryInfo(p.getCategoryName() != null ? p.getCategoryName() : ""))
                .tags(p.getTags())
                .relatedImages(p.getRelatedImages())
                .colors(p.getColors())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
