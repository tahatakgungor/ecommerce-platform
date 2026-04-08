package com.ecommerce.product.event;

import com.ecommerce.product.domain.Order;
import lombok.Getter;

@Getter
public class OrderPlacedEvent {
    private final Order order;

    public OrderPlacedEvent(Order order) {
        this.order = order;
    }
}
