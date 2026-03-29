package com.ecommerce.product.repository;

import com.ecommerce.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByStatus(String status);

    @Query("SELECT p FROM Product p WHERE p.price < p.originalPrice AND p.status = 'Active'")
    List<Product> findDiscountProducts();

    @Query("SELECT DISTINCT p FROM Product p JOIN p.tags t WHERE t IN :tags AND p.status = 'Active'")
    List<Product> findRelatedProducts(@Param("tags") List<String> tags);
}
