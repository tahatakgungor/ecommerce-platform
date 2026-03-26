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
    private final KafkaTemplate<String, Object> kafkaTemplate; // Kafka için gerekli araç

    @Transactional
    public Product save(Product product) {
        Product savedProduct = productRepository.save(product);

        // Profesyonel Event Nesnesi Oluşturma
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(savedProduct.getId())
                .name(savedProduct.getName())
                .price(savedProduct.getPrice())
                .createdAt(LocalDateTime.now())
                .build();

        log.info("Kafka'ya Event fırlatılıyor: {}", event.getProductId());
        //kafkaTemplate.send("product-created-events", event.getProductId().toString(), event);

        return savedProduct;
    }

    // Long yerine UUID kullanıyoruz
    public Product findById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı! ID: " + id));
    }

    @Transactional
    public Product update(UUID id, Product productDetails) {
        Product existingProduct = findById(id);

        existingProduct.setName(productDetails.getName());
        existingProduct.setDescription(productDetails.getDescription());
        existingProduct.setPrice(productDetails.getPrice());
        // setStock yerine setStockQuantity kullanıyoruz
        existingProduct.setStockQuantity(productDetails.getStockQuantity());

        log.info("Ürün güncellendi: {}", id);
        return productRepository.save(existingProduct);
    }

    @Transactional
    public void delete(UUID id) {
        Product product = findById(id);
        productRepository.delete(product);
        log.info("Ürün silindi: {}", id);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }
}