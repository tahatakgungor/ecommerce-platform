package com.ecommerce.product.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    // Özel sorgular gerekirse buraya yazacağız
}
