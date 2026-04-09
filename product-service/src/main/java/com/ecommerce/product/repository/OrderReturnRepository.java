package com.ecommerce.product.repository;

import com.ecommerce.product.domain.OrderReturn;
import com.ecommerce.product.domain.OrderReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderReturnRepository extends JpaRepository<OrderReturn, UUID> {
    List<OrderReturn> findByUserEmailIgnoreCaseOrderByCreatedAtDesc(String userEmail);
    List<OrderReturn> findAllByOrderByCreatedAtDesc();
    Optional<OrderReturn> findByOrderId(UUID orderId);
    boolean existsByOrderIdAndStatusIn(UUID orderId, Collection<OrderReturnStatus> statuses);
}
