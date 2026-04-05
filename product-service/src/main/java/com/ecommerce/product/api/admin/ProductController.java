package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.ProductService;
import com.ecommerce.product.domain.Product;
import com.ecommerce.product.dto.ApiResponse;
import com.ecommerce.product.dto.product.ProductRequest;
import com.ecommerce.product.dto.product.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    /**
     * Tüm ürünleri listeler (Admin Paneli Tablosu için)
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<List<ProductResponse>> getAllProducts() {
        List<Product> products = productService.findAll();

        // Entity listesini Response DTO listesine çeviriyoruz (Güvenli ve Harri uyumlu)
        List<ProductResponse> responses = products.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ApiResponse.ok(responses, (long) responses.size());
    }

    /**
     * Yeni ürün ekler
     */
    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<ProductResponse> createProduct(@RequestBody ProductRequest request) {
        log.info("Yeni ürün ekleniyor: {}", request.getName());
        Product product = convertToEntity(request);
        Product saved = productService.save(product);
        return ApiResponse.ok(convertToResponse(saved), 1L);
    }

    /**
     * ID ile tekil ürün getirir (Düzenleme ekranı için)
     */
    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable("id") UUID id) {
        Product product = productService.findById(id);
        return ApiResponse.ok(convertToResponse(product), 1L);
    }

    /**
     * Mevcut ürünü günceller
     */
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable("id") UUID id, @RequestBody ProductRequest request) {
        try {
            Product product = convertToEntity(request);
            Product updated = productService.update(id, product);
            return ApiResponse.ok(convertToResponse(updated), 1L);
        } catch (Exception ex) {
            log.error("Ürün güncelleme hatası. id={} request={}", id, request, ex);
            throw ex;
        }
    }

    /**
     * Ürünü siler
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<String> deleteProduct(@PathVariable("id") UUID id) {
        productService.delete(id);
        return ApiResponse.ok("Ürün başarıyla silindi!", 1L);
    }

    // --- MAPPING LOGIC ---

    /**
     * Entity'den DTO'ya: Frontend'e veri gönderirken (Response)
     */
    private ProductResponse convertToResponse(Product product) {
        if (product == null) return null;
        double price = product.getPrice() != null ? product.getPrice() : 0.0;
        double originalPrice = product.getOriginalPrice() != null ? product.getOriginalPrice() : price;
        int discount = 0;
        if (originalPrice > 0 && originalPrice > price) {
            discount = (int) Math.round((1 - price / originalPrice) * 100);
        }
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(price)
                .originalPrice(originalPrice)
                .discount(discount)
                .stockQuantity(product.getStockQuantity() != null ? product.getStockQuantity() : 0)
                .sku(product.getSku())
                .image(product.getImage())
                .status(product.getStatus())
                .parentCategory(product.getParentCategory())
                .childCategory(product.getChildCategory())
                .brand(new ProductResponse.BrandInfo(product.getBrandName() != null ? product.getBrandName() : ""))
                .category(new ProductResponse.CategoryInfo(product.getCategoryName() != null ? product.getCategoryName() : ""))
                .tags(product.getTags())
                .relatedImages(product.getRelatedImages())
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
        if (request.getTags() != null) {
            product.setTags(new ArrayList<>(request.getTags()));
        } else {
            product.setTags(null);
        }
        if (request.getColors() != null) {
            product.setColors(new ArrayList<>(request.getColors()));
        } else {
            product.setColors(null);
        }

        // Fiyat Ayarı
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getOriginalPrice() != null && request.getOriginalPrice() > 0) {
            product.setOriginalPrice(request.getOriginalPrice());
        }

        // --- ESNEK VERİ AYIKLAMA (String veya Object) ---
        product.setParentCategory(extractString(request.getParent()));
        product.setChildCategory(extractString(request.getChildren()));
        product.setCategoryName(extractString(request.getCategory()));
        product.setBrandName(extractString(request.getBrand()));
        product.setImage(extractString(request.getImage()));
        if (request.getRelatedImages() != null) {
            product.setRelatedImages(normalizeStringList(request.getRelatedImages()));
        } else {
            product.setRelatedImages(null);
        }
        if (request.getStatus() != null) product.setStatus(request.getStatus());

        return product;
    }

    /**
     * Admin panelinden veya Client'tan gelen verinin tipine bakmaksızın
     * (String, URL objesi, Kategori objesi vb.) ham metni dışarı çıkarır.
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

    private List<String> normalizeStringList(List<String> values) {
        if (values == null) return null;
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
