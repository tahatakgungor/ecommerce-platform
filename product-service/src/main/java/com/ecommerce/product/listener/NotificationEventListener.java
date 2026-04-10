package com.ecommerce.product.listener;

import com.ecommerce.product.application.EmailService;
import com.ecommerce.product.application.ActivityLogService;
import com.ecommerce.product.event.OrderDeliveredEvent;
import com.ecommerce.product.event.OrderPlacedEvent;
import com.ecommerce.product.event.OrderReturnCreatedEvent;
import com.ecommerce.product.event.OrderReturnStatusChangedEvent;
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

    @Async
    @EventListener
    public void handleOrderDeliveredEvent(OrderDeliveredEvent event) {
        String eventId = java.util.UUID.randomUUID().toString();
        log.info("[EmailEvent:{}] Handling OrderDeliveredEvent for invoice={}", eventId, event.getOrder().getInvoice());
        try {
            emailService.sendDeliveryConfirmation(event.getOrder());
            activityLogService.log(
                    "ORDER_DELIVERED_NOTIFICATION",
                    "INFO",
                    "Teslim bildirimi e-postası gönderildi.",
                    "system",
                    "ORDER",
                    event.getOrder().getId() != null ? event.getOrder().getId().toString() : null,
                    java.util.Map.of("invoice", event.getOrder().getInvoice() != null ? event.getOrder().getInvoice() : "")
            );
        } catch (Exception e) {
            log.warn("[EmailEvent:{}] OrderDeliveredEvent işlenirken hata: {}", eventId, e.getMessage());
        }
        log.info("[EmailEvent:{}] OrderDeliveredEvent completed for invoice={}", eventId, event.getOrder().getInvoice());
    }

    @Async
    @EventListener
    public void handleOrderReturnCreatedEvent(OrderReturnCreatedEvent event) {
        String eventId = UUID.randomUUID().toString();
        log.info("[EmailEvent:{}] Handling OrderReturnCreatedEvent for returnId={}", eventId, event.getOrderReturn().getId());
        try {
            emailService.sendReturnRequestConfirmation(event.getOrderReturn(), event.getOrder());
            emailService.sendReturnAdminAlert(event.getOrderReturn(), event.getOrder());
            activityLogService.log(
                    "RETURN_NOTIFICATION",
                    "INFO",
                    "İade talebi için müşteri ve admin bildirimleri gönderildi.",
                    "system",
                    "ORDER_RETURN",
                    event.getOrderReturn().getId() != null ? event.getOrderReturn().getId().toString() : null,
                    java.util.Map.of("orderId", event.getOrder().getId() != null ? event.getOrder().getId().toString() : "")
            );
        } catch (Exception e) {
            log.warn("[EmailEvent:{}] OrderReturnCreatedEvent işlenirken hata: {}", eventId, e.getMessage());
        }
        log.info("[EmailEvent:{}] OrderReturnCreatedEvent completed for returnId={}", eventId, event.getOrderReturn().getId());
    }

    @Async
    @EventListener
    public void handleOrderReturnStatusChangedEvent(OrderReturnStatusChangedEvent event) {
        String eventId = UUID.randomUUID().toString();
        log.info("[EmailEvent:{}] Handling OrderReturnStatusChangedEvent for returnId={}, status={}", eventId,
                event.getOrderReturn().getId(), event.getOrderReturn().getStatus());
        try {
            emailService.sendReturnStatusUpdate(event.getOrderReturn(), event.getOrder());
            activityLogService.log(
                    "RETURN_NOTIFICATION",
                    "INFO",
                    "İade durum güncellemesi müşteriye bildirildi.",
                    "system",
                    "ORDER_RETURN",
                    event.getOrderReturn().getId() != null ? event.getOrderReturn().getId().toString() : null,
                    java.util.Map.of("newStatus", event.getOrderReturn().getStatus().name())
            );
        } catch (Exception e) {
            log.warn("[EmailEvent:{}] OrderReturnStatusChangedEvent işlenirken hata: {}", eventId, e.getMessage());
        }
        log.info("[EmailEvent:{}] OrderReturnStatusChangedEvent completed for returnId={}", eventId, event.getOrderReturn().getId());
    }
}
