package com.ecommerce.product.repository;

import com.ecommerce.product.domain.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {
    List<ActivityLog> findTop200ByOrderByCreatedAtDesc();
    List<ActivityLog> findTop200ByEventTypeOrderByCreatedAtDesc(String eventType);
}
