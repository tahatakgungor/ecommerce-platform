package com.ecommerce.product.application.event;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ProductCreatedEvent {
    private UUID productId;
    private String name;
    private BigDecimal price;
    private LocalDateTime createdAt;
}