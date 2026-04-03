package com.ecommerce.product.repository;

import com.ecommerce.product.domain.NewsletterEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsletterRepository extends JpaRepository<NewsletterEmail, Long> {
    Optional<NewsletterEmail> findByEmail(String email);
}
