package com.ecommerce.product.listener;

import com.ecommerce.product.application.EmailService;
import com.ecommerce.product.event.OrderPlacedEvent;
import com.ecommerce.product.event.OrderShippedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final EmailService emailService;

    @Async
    @EventListener
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        String eventId = UUID.randomUUID().toString();
        log.info("[EmailEvent:{}] Handling OrderPlacedEvent for invoice={}", eventId, event.getOrder().getInvoice());
        emailService.sendOrderConfirmation(event.getOrder());
        log.info("[EmailEvent:{}] OrderPlacedEvent completed for invoice={}", eventId, event.getOrder().getInvoice());
        // TODO: smsService.sendOrderPlacedSms(event.getOrder());
    }

    @Async
    @EventListener
    public void handleOrderShippedEvent(OrderShippedEvent event) {
        String eventId = UUID.randomUUID().toString();
        log.info("[EmailEvent:{}] Handling OrderShippedEvent for invoice={}", eventId, event.getOrder().getInvoice());
        emailService.sendShippingUpdate(event.getOrder());
        log.info("[EmailEvent:{}] OrderShippedEvent completed for invoice={}", eventId, event.getOrder().getInvoice());
        // TODO: smsService.sendOrderShippedSms(event.getOrder());
    }
}
