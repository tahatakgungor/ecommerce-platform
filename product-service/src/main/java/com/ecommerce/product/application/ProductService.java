package com.ecommerce.product.application;

import com.ecommerce.product.application.event.ProductCreatedEvent;
import com.ecommerce.product.domain.Product;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Yeni bir ürün kaydeder ve süreci Kafka üzerinden diğer servislere duyurur.
     */
    @Transactional
    public Product save(Product product) {
        // 1. İş Mantığı ve Varsayılan Değerler (Data Consistency)
        applyDefaultValues(product);

        // 2. Veritabanına Kayıt (Persistence)
        Product savedProduct = productRepository.save(product);
        log.info("Ürün veritabanına kaydedildi: {}, ID: {}", savedProduct.getName(), savedProduct.getId());

        // 3. Kafka Event (Event-Driven Architecture)
        // Kafka'yı ayrı bir metodda yönetmek kodun okunabilirliğini artırır.
        sendProductCreatedEvent(savedProduct);

        return savedProduct;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı! ID: " + id));
    }

    /**
     * Mevcut ürünü günceller. Sadece değişen alanları (Partial Update) dikkate alır.
     */
    @Transactional
    public Product update(UUID id, Product details) {
        Product existing = findById(id);

        // Dinamik Alan Güncellemeleri (Null-Safe)
        if (details.getName() != null) existing.setName(details.getName());
        if (details.getDescription() != null) existing.setDescription(details.getDescription());
        if (details.getPrice() != null) {
            existing.setPrice(details.getPrice());
            existing.setOriginalPrice(details.getPrice()); // Frontend uyumu için önemli
        }
        if (details.getStockQuantity() != null) existing.setStockQuantity(details.getStockQuantity());
        if (details.getSku() != null) existing.setSku(details.getSku());
        if (details.getImage() != null) existing.setImage(details.getImage());
        if (details.getStatus() != null) existing.setStatus(details.getStatus());

        // Harri Özel Alanları
        if (details.getParentCategory() != null) existing.setParentCategory(details.getParentCategory());
        if (details.getChildCategory() != null) existing.setChildCategory(details.getChildCategory());
        if (details.getCategoryName() != null) existing.setCategoryName(details.getCategoryName());
        if (details.getBrandName() != null) existing.setBrandName(details.getBrandName());

        // Koleksiyon Güncellemeleri
        if (details.getTags() != null) existing.setTags(details.getTags());
        if (details.getColors() != null) existing.setColors(details.getColors());

        log.info("Ürün başarıyla güncellendi. ID: {}", id);
        return productRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Silinmek istenen ürün bulunamadı! ID: " + id);
        }
        productRepository.deleteById(id);
        log.info("Ürün başarıyla silindi. ID: {}", id);
    }

    // --- Private Yardımcı Metodlar (Modülerlik için) ---

    private void applyDefaultValues(Product product) {
        if (product.getSku() == null || product.getSku().isBlank()) {
            product.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (product.getImage() == null || product.getImage().isBlank()) {
            product.setImage("https://via.placeholder.com/150");
        }
        if (product.getStatus() == null || product.getStatus().isBlank()) {
            product.setStatus("Active");
        }
        if (product.getPrice() == null) product.setPrice(0.0);
        if (product.getOriginalPrice() == null) product.setOriginalPrice(product.getPrice());
    }

    private void sendProductCreatedEvent(Product savedProduct) {
        try {
            ProductCreatedEvent event = ProductCreatedEvent.builder()
                    .productId(savedProduct.getId())
                    .name(savedProduct.getName())
                    .sku(savedProduct.getSku())
                    .price(savedProduct.getPrice())
                    .stock(savedProduct.getStockQuantity())
                    .status(savedProduct.getStatus())
                    .createdAt(LocalDateTime.now())
                    .build();

            log.info("Kafka'ya mesaj gönderiliyor: {}", event.getSku());
            // kafkaTemplate.send("product-created-events", savedProduct.getId().toString(), event);
        } catch (Exception e) {
            log.error("Kafka gönderim hatası (İşlem iptal edilmiyor): {}", e.getMessage());
        }
    }
}