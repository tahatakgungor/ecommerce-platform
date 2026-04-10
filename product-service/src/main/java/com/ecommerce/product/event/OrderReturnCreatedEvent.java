package com.ecommerce.product.event;

import com.ecommerce.product.domain.Order;
import com.ecommerce.product.domain.OrderReturn;
import lombok.Getter;

@Getter
public class OrderReturnCreatedEvent {
    private final OrderReturn orderReturn;
    private final Order order;

    public OrderReturnCreatedEvent(OrderReturn orderReturn, Order order) {
        this.orderReturn = orderReturn;
        this.order = order;
    }
}
