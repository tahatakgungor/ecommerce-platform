package com.ecommerce.product.repository;

import com.ecommerce.product.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Order> findByCreatedAtAfter(LocalDateTime date);
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByStatusIgnoreCase(String status);
    List<Order> findByStatusIgnoreCaseAndShippedAtBefore(String status, LocalDateTime cutoff);
    Optional<Order> findTopByIyzicoTokenOrderByCreatedAtDesc(String iyzicoToken);
    Optional<Order> findTopByIyzicoConversationIdOrderByCreatedAtDesc(String iyzicoConversationId);
    Optional<Order> findByInvoiceAndEmail(String invoice, String email);
    Optional<Order> findByInvoiceAndEmailIgnoreCase(String invoice, String email);
    Optional<Order> findByInvoiceAndGuestEmailIgnoreCase(String invoice, String guestEmail);
    List<Order> findByGuestEmailIgnoreCaseOrderByCreatedAtDesc(String guestEmail);
}
