package com.ecommerce.product.event;

import com.ecommerce.product.domain.Order;
import lombok.Getter;

@Getter
public class OrderDeliveredEvent {
    private final Order order;

    public OrderDeliveredEvent(Order order) {
        this.order = order;
    }
}
