package com.ecommerce.product.repository;

import com.ecommerce.product.domain.ProductReviewFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductReviewFeedbackRepository extends JpaRepository<ProductReviewFeedback, UUID> {
    Optional<ProductReviewFeedback> findByReviewIdAndUserId(UUID reviewId, UUID userId);
    void deleteByReviewId(UUID reviewId);
    void deleteByUserId(UUID userId);
}
