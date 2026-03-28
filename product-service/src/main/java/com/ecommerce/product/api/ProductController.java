package com.ecommerce.product.api;

import com.ecommerce.product.application.ProductService;
import com.ecommerce.product.domain.Product;
import com.ecommerce.product.dto.ApiResponse;
import com.ecommerce.product.dto.product.ProductRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @GetMapping("/all")
    public ApiResponse<List<Product>> getAllProducts() {
        List<Product> products = productService.findAll();

        // KRİTİK DOKUNUŞ: Veritabanındaki eski null veriler tabloyu bozmasın
        products.forEach(p -> {
            if (p.getPrice() == null) p.setPrice(p.getOriginalPrice() != null ? p.getOriginalPrice() : 0.0);
            if (p.getOriginalPrice() == null) p.setOriginalPrice(p.getPrice());
        });

        return new ApiResponse<>(true, products, (long) products.size());
    }

    @PostMapping("/add")
    public ApiResponse<Product> createProduct(@RequestBody ProductRequest request) {
        log.info("Yeni ürün ekleniyor: {}", request.getName());
        Product product = convertToEntity(request);
        Product saved = productService.save(product);
        return new ApiResponse<>(true, saved, 1L);
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> getProductById(@PathVariable("id") UUID id) {
        Product product = productService.findById(id);
        return new ApiResponse<>(true, product, 1L);
    }

    private Product convertToEntity(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setStockQuantity(request.getStockQuantity());
        product.setSku(request.getSku());
        product.setParentCategory(request.getParent());
        product.setChildCategory(request.getChildren());
        product.setTags(request.getTags());
        product.setColors(request.getColors());

        // Fiyat Güvenliği: Frontend'den 'price' gelir, biz her iki alanı da doldururuz.
        if (request.getPrice() != null) {
            Double pValue = request.getPrice();
            product.setPrice(pValue);         // Tablo (List) için
            product.setOriginalPrice(pValue); // Grid/Detay (toFixed) için
        } else {
            product.setPrice(0.0);
            product.setOriginalPrice(0.0);
        }

        // Resim Kontrolü
        if (request.getImage() instanceof String) {
            product.setImage((String) request.getImage());
        } else if (request.getImage() instanceof Map) {
            Map<?, ?> imgMap = (Map<?, ?>) request.getImage();
            product.setImage((String) imgMap.get("url"));
        }

        return product;
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteProduct(@PathVariable("id") UUID id) {
        productService.delete(id);
        return new ApiResponse<>(true, "Ürün başarıyla silindi!", 1L);
    }
}