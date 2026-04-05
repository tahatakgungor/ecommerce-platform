package com.ecommerce.product.application;

import com.ecommerce.product.domain.Order;
import com.ecommerce.product.domain.Product;
import com.ecommerce.product.domain.ReviewStatus;
import com.ecommerce.product.dto.product.ProductResponse;
import com.ecommerce.product.repository.OrderRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ProductReviewRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ProductReviewRepository productReviewRepository;
    private final ObjectMapper objectMapper;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı! ID: " + id));
    }

    public List<ProductResponse> findPopularProducts(String type, Integer limit) {
        int safeLimit = limit == null ? 8 : Math.max(1, Math.min(50, limit));
        String normalizedType = (type == null || type.isBlank())
                ? "top-rated"
                : type.trim().toLowerCase(Locale.ROOT);

        return switch (normalizedType) {
            case "best-selling" -> mapProducts(findBestSellingProducts(safeLimit));
            case "latest" -> mapProducts(findLatestProducts(safeLimit));
            default -> mapProducts(findTopRatedProducts(safeLimit));
        };
    }

    @Transactional
    @PreAuthorize("hasAuthority('Admin')") // GÜVENLİK: Sadece Admin ekleyebilir
    public Product save(Product product) {
        applyDefaultValues(product);
        Product savedProduct = productRepository.save(product);
        log.info("Yeni ürün Admin tarafından kaydedildi: {}", savedProduct.getName());
        return savedProduct;
    }

    @Transactional
    @PreAuthorize("hasAuthority('Admin')") // GÜVENLİK: Sadece Admin güncelleyebilir
    public Product update(UUID id, Product details) {
        Product existing = findById(id);

        if (details.getName() != null) existing.setName(details.getName());
        if (details.getDescription() != null) existing.setDescription(details.getDescription());
        if (details.getPrice() != null) {
            existing.setPrice(details.getPrice());
        }
        if (details.getOriginalPrice() != null && details.getOriginalPrice() > 0) {
            existing.setOriginalPrice(details.getOriginalPrice());
        }
        if (details.getStockQuantity() != null) existing.setStockQuantity(details.getStockQuantity());
        if (details.getSku() != null) existing.setSku(details.getSku());
        if (details.getImage() != null) existing.setImage(details.getImage());
        if (details.getStatus() != null) existing.setStatus(details.getStatus());
        if (details.getParentCategory() != null) existing.setParentCategory(details.getParentCategory());
        if (details.getChildCategory() != null) existing.setChildCategory(details.getChildCategory());
        if (details.getCategoryName() != null) existing.setCategoryName(details.getCategoryName());
        if (details.getBrandName() != null) existing.setBrandName(details.getBrandName());
        if (details.getTags() != null) existing.setTags(details.getTags());
        if (details.getRelatedImages() != null) existing.setRelatedImages(details.getRelatedImages());
        if (details.getColors() != null) existing.setColors(details.getColors());

        // İndirim yüzdesinin negatif olmaması için güvenlik:
        // originalPrice hiçbir durumda price'dan küçük kalmasın.
        if (existing.getPrice() != null && existing.getOriginalPrice() != null
                && existing.getOriginalPrice() < existing.getPrice()) {
            existing.setOriginalPrice(existing.getPrice());
        }

        return productRepository.save(existing);
    }

    @Transactional
    @PreAuthorize("hasAuthority('Admin')") // GÜVENLİK: Sadece Admin silebilir
    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Silinecek ürün bulunamadı!");
        }
        productRepository.deleteById(id);
        log.info("Ürün başarıyla silindi. ID: {}", id);
    }

    private void applyDefaultValues(Product product) {
        if (product.getSku() == null || product.getSku().isBlank()) {
            product.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (product.getStatus() == null) product.setStatus("Active");
        if (product.getPrice() == null) product.setPrice(0.0);
        if (product.getOriginalPrice() == null) product.setOriginalPrice(product.getPrice());
    }

    private List<Product> findLatestProducts(int limit) {
        return productRepository.findByStatusOrderByCreatedAtDesc("Active")
                .stream()
                .limit(limit)
                .toList();
    }

    private List<Product> findTopRatedProducts(int limit) {
        List<Product> activeProducts = productRepository.findByStatus("Active");
        Map<UUID, ProductRatingScore> scoreMap = new LinkedHashMap<>();

        for (Product product : activeProducts) {
            List<Object[]> rows = productReviewRepository.findAverageAndCountByProductAndStatus(
                    product.getId(),
                    ReviewStatus.APPROVED
            );

            double avg = 0.0;
            long count = 0L;
            if (!rows.isEmpty()) {
                Object[] row = rows.get(0);
                avg = toDouble(row[0]);
                count = toLong(row[1]);
            }
            scoreMap.put(product.getId(), new ProductRatingScore(avg, count));
        }

        return activeProducts.stream()
                .sorted(Comparator
                        .comparing((Product p) -> scoreMap.get(p.getId()).avg()).reversed()
                        .thenComparing((Product p) -> scoreMap.get(p.getId()).count(), Comparator.reverseOrder())
                        .thenComparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private List<Product> findBestSellingProducts(int limit) {
        List<Product> activeProducts = productRepository.findByStatus("Active");
        Map<UUID, Product> productMap = activeProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a, LinkedHashMap::new));
        Map<UUID, Integer> soldQuantity = new LinkedHashMap<>();

        List<Order> deliveredOrders = orderRepository.findByStatusIgnoreCase("delivered");
        for (Order order : deliveredOrders) {
            for (Map<String, Object> item : parseCartItems(order.getCart())) {
                UUID productId = parseUuid(item.get("_id"), item.get("id"));
                if (productId == null || !productMap.containsKey(productId)) {
                    continue;
                }
                int quantity = parseQuantity(item.get("orderQuantity"));
                soldQuantity.merge(productId, quantity, Integer::sum);
            }
        }

        return activeProducts.stream()
                .sorted(Comparator
                        .comparing((Product p) -> soldQuantity.getOrDefault(p.getId(), 0), Comparator.reverseOrder())
                        .thenComparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private List<ProductResponse> mapProducts(List<Product> products) {
        return products.stream().map(this::toResponse).toList();
    }

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

    private List<Map<String, Object>> parseCartItems(String cartJson) {
        if (cartJson == null || cartJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(cartJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private UUID parseUuid(Object... values) {
        for (Object value : values) {
            if (value == null) continue;
            try {
                return UUID.fromString(String.valueOf(value));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    private int parseQuantity(Object value) {
        if (value == null) return 0;
        try {
            int quantity = Integer.parseInt(String.valueOf(value));
            return Math.max(0, quantity);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private record ProductRatingScore(double avg, long count) {
    }
}
