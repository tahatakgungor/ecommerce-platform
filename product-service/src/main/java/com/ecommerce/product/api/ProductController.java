package com.ecommerce.product.api;

import com.ecommerce.product.application.ProductService;
import com.ecommerce.product.domain.Product;
import com.ecommerce.product.dto.ApiResponse;
import com.ecommerce.product.dto.product.ProductRequest;
import com.ecommerce.product.dto.product.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @GetMapping("/all")
    public ApiResponse<List<ProductResponse>> getAllProducts() {
        List<Product> products = productService.findAll();

        // Entity listesini Response DTO listesine çeviriyoruz (Güvenli ve Harri uyumlu)
        List<ProductResponse> responses = products.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ApiResponse.ok(responses, (long) responses.size());
    }

    @PostMapping("/add")
    public ApiResponse<ProductResponse> createProduct(@RequestBody ProductRequest request) {
        log.info("Yeni ürün ekleniyor: {}", request.getName());
        Product product = convertToEntity(request);
        Product saved = productService.save(product);
        return ApiResponse.ok(convertToResponse(saved), 1L);
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable("id") UUID id) {
        Product product = productService.findById(id);
        return ApiResponse.ok(convertToResponse(product), 1L);
    }

    @PutMapping("/update/{id}")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable("id") UUID id, @RequestBody ProductRequest request) {
        Product product = convertToEntity(request);
        Product updated = productService.update(id, product);
        return ApiResponse.ok(convertToResponse(updated), 1L);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteProduct(@PathVariable("id") UUID id) {
        productService.delete(id);
        return ApiResponse.ok("Ürün başarıyla silindi!", 1L);
    }

    // --- MAPPING METODLARI (PRO LEVEL) ---

    /**
     * Entity'den DTO'ya: Frontend'e veri gönderirken (Response)
     */
    private ProductResponse convertToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice() != null ? product.getPrice() : 0.0)
                // Harri şablonunun en hassas olduğu nokta: originalPrice asla null gelmemeli
                .originalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice() :
                        (product.getPrice() != null ? product.getPrice() : 0.0))
                .stockQuantity(product.getStockQuantity() != null ? product.getStockQuantity() : 0)
                .sku(product.getSku())
                .image(product.getImage())
                .status(product.getStatus())
                .parentCategory(product.getParentCategory())
                .childCategory(product.getChildCategory())
                .categoryName(product.getCategoryName())
                .brandName(product.getBrandName())
                .tags(product.getTags())
                .colors(product.getColors())
                .build();
    }

    /**
     * DTO'dan Entity'ye: Frontend'den veri alırken (Request)
     */
    private Product convertToEntity(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setStockQuantity(request.getStockQuantity());
        product.setSku(request.getSku());
        product.setTags(request.getTags());
        product.setColors(request.getColors());

        // Fiyat Ayarı
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
            product.setOriginalPrice(request.getPrice());
        }

        // --- ESNEK VERİ AYIKLAMA (String veya Object) ---
        product.setParentCategory(extractString(request.getParent()));
        product.setChildCategory(extractString(request.getChildren()));
        product.setCategoryName(extractString(request.getCategory()));
        product.setBrandName(extractString(request.getBrand()));
        product.setImage(extractString(request.getImage()));

        return product;
    }

    /**
     * Gelen veri String ise direkt döner, Map (Object) ise içindeki 'name' veya 'url' alanını bulur.
     */
    private String extractString(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String) return (String) obj;
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            // Kategori/Brand için genelde 'name', Resim için 'url' kullanılır
            if (map.containsKey("name")) return (String) map.get("name");
            if (map.containsKey("url")) return (String) map.get("url");
            if (map.containsKey("title")) return (String) map.get("title");
            // Eğer hiçbirini bulamazsa toString dene veya ilk değeri al
            return map.values().stream().findFirst().map(Object::toString).orElse(null);
        }
        return obj.toString();
    }
}