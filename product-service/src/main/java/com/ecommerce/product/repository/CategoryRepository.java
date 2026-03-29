package com.ecommerce.product.repository;

import com.ecommerce.product.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByName(String name);

    // Belirli bir kategori ismine ait ürün sayısını çekmek için (Opsiyonel/İlerisi için)
    @Query("SELECT COUNT(p) FROM Product p WHERE p.categoryName = :name")
    long countProductsByCategoryName(@Param("name") String name);
}