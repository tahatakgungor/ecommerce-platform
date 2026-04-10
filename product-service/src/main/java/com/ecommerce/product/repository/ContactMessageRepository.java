package com.ecommerce.product.repository;

import com.ecommerce.product.domain.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, UUID> {
    Page<ContactMessage> findByStatusIgnoreCaseOrderByCreatedAtDesc(String status, Pageable pageable);
}
