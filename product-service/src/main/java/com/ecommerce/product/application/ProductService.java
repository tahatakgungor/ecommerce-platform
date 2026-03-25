package com.ecommerce.product.application;

import com.ecommerce.product.application.event.ProductCreatedEvent;
import com.ecommerce.product.domain.Product;
import com.ecommerce.product.domain.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    public List<Product> findAll() {
        return productRepository.findAll();
    }
}