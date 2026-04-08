package com.ecommerce.product.listener;

import com.ecommerce.product.application.EmailService;
import com.ecommerce.product.event.OrderPlacedEvent;
import com.ecommerce.product.event.OrderShippedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final EmailService emailService;

    @Async
    @EventListener
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        log.info("Handling OrderPlacedEvent for order: {}", event.getOrder().getInvoice());
        emailService.sendOrderConfirmation(event.getOrder());
        // TODO: smsService.sendOrderPlacedSms(event.getOrder());
    }

    @Async
    @EventListener
    public void handleOrderShippedEvent(OrderShippedEvent event) {
        log.info("Handling OrderShippedEvent for order: {}", event.getOrder().getInvoice());
        emailService.sendShippingUpdate(event.getOrder());
        // TODO: smsService.sendOrderShippedSms(event.getOrder());
    }
}
