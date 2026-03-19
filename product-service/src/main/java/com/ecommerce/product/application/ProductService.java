package com.ecommerce.product.application;

import com.ecommerce.product.domain.Product;
import com.ecommerce.product.domain.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // Kafka'ya mesaj gönderiyoruz
        log.info("Ürün kaydedildi, Kafka'ya event gönderiliyor: {}", savedProduct.getId());
        kafkaTemplate.send("product-created-events", savedProduct.getId().toString(), savedProduct);

        return savedProduct;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }
}