package com.ecommerce.product.listener;

import com.ecommerce.product.application.EmailService;
import com.ecommerce.product.application.ActivityLogService;
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
    private final ActivityLogService activityLogService;

    @Async
    @EventListener
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        String eventId = UUID.randomUUID().toString();
        log.info("[EmailEvent:{}] Handling OrderPlacedEvent for invoice={}", eventId, event.getOrder().getInvoice());
        emailService.sendOrderConfirmation(event.getOrder());
        emailService.sendNewOrderAlertToAdmins(event.getOrder());
        activityLogService.log(
                "ORDER_NOTIFICATION",
                "INFO",
                "Yeni sipariş için müşteri ve admin bildirimleri gönderildi.",
                "system",
                "ORDER",
                event.getOrder().getId() != null ? event.getOrder().getId().toString() : null,
                java.util.Map.of("invoice", event.getOrder().getInvoice() != null ? event.getOrder().getInvoice() : "")
        );
        log.info("[EmailEvent:{}] OrderPlacedEvent completed for invoice={}", eventId, event.getOrder().getInvoice());
        // TODO: smsService.sendOrderPlacedSms(event.getOrder());
    }

    @Async
    @EventListener
    public void handleOrderShippedEvent(OrderShippedEvent event) {
        String eventId = UUID.randomUUID().toString();
        log.info("[EmailEvent:{}] Handling OrderShippedEvent for invoice={}", eventId, event.getOrder().getInvoice());
        emailService.sendShippingUpdate(event.getOrder());
        activityLogService.log(
                "ORDER_NOTIFICATION",
                "INFO",
                "Kargo bilgilendirme e-postası gönderildi.",
                "system",
                "ORDER",
                event.getOrder().getId() != null ? event.getOrder().getId().toString() : null,
                java.util.Map.of("invoice", event.getOrder().getInvoice() != null ? event.getOrder().getInvoice() : "")
        );
        log.info("[EmailEvent:{}] OrderShippedEvent completed for invoice={}", eventId, event.getOrder().getInvoice());
        // TODO: smsService.sendOrderShippedSms(event.getOrder());
    }
}
