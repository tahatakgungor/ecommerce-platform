package com.ecommerce.product.event;

import com.ecommerce.product.domain.Order;
import com.ecommerce.product.domain.OrderReturn;
import lombok.Getter;

@Getter
public class OrderReturnStatusChangedEvent {
    private final OrderReturn orderReturn;
    private final Order order;

    public OrderReturnStatusChangedEvent(OrderReturn orderReturn, Order order) {
        this.orderReturn = orderReturn;
        this.order = order;
    }
}
