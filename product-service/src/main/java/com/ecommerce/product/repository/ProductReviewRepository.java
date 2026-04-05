package com.ecommerce.product.repository;

import com.ecommerce.product.domain.ProductReview;
import com.ecommerce.product.domain.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {

    Optional<ProductReview> findByProductIdAndUserId(UUID productId, UUID userId);

    Optional<ProductReview> findByIdAndProductId(UUID reviewId, UUID productId);

    List<ProductReview> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    List<ProductReview> findByProductId(UUID productId);

    Page<ProductReview> findByProductIdAndStatus(UUID productId, ReviewStatus status, Pageable pageable);

    @Query("""
            SELECT r FROM ProductReview r
            WHERE r.product.id = :productId
              AND r.status = :status
              AND r.mediaUrls IS NOT NULL
              AND r.mediaUrls <> ''
              AND r.mediaUrls <> '[]'
            """)
    Page<ProductReview> findByProductIdAndStatusWithMedia(@Param("productId") UUID productId,
                                                          @Param("status") ReviewStatus status,
                                                          Pageable pageable);

    Page<ProductReview> findByStatus(ReviewStatus status, Pageable pageable);

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0), COUNT(r)
            FROM ProductReview r
            WHERE r.product.id = :productId AND r.status = :status
            """)
    List<Object[]> findAverageAndCountByProductAndStatus(@Param("productId") UUID productId,
                                                         @Param("status") ReviewStatus status);

    @Query("""
            SELECT r.rating, COUNT(r)
            FROM ProductReview r
            WHERE r.product.id = :productId AND r.status = :status
            GROUP BY r.rating
            """)
    List<Object[]> findRatingDistributionByProductAndStatus(@Param("productId") UUID productId,
                                                            @Param("status") ReviewStatus status);

    long countByProductIdAndStatus(UUID productId, ReviewStatus status);
}
