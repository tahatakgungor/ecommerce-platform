package com.ecommerce.product.event;

import com.ecommerce.product.domain.Order;
import lombok.Getter;

@Getter
public class OrderShippedEvent {
    private final Order order;

    public OrderShippedEvent(Order order) {
        this.order = order;
    }
}
