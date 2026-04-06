package com.ecommerce.product.repository;

import com.ecommerce.product.domain.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {
    List<BlogPost> findAllByOrderByUpdatedAtDesc();
    List<BlogPost> findByStatusOrderByPublishedAtDescUpdatedAtDesc(String status);
    Optional<BlogPost> findBySlugAndStatus(String slug, String status);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);
}

