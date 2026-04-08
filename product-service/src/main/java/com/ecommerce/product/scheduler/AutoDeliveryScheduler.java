package com.ecommerce.product.scheduler;

import com.ecommerce.product.application.EmailService;
import com.ecommerce.product.domain.Order;
import com.ecommerce.product.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Kargoya verilmiş ve belirtilen gün sayısını geçmiş siparişleri
 * otomatik olarak "delivered" durumuna taşır.
 *
 * Varsayılan: kargo tarihinden itibaren 10 gün sonra.
 * application.properties içinde: app.shipping.auto-deliver-days=10
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AutoDeliveryScheduler {

    private final OrderRepository orderRepository;
    private final EmailService emailService;

    @Value("${app.shipping.auto-deliver-days:10}")
    private int autoDeliverDays;

    /**
     * Her gün saat 02:00'de çalışır.
     * Cron: saniye dakika saat gün ay haftaGünü
     */
    @Scheduled(
            cron = "${app.shipping.auto-deliver-cron:0 0 2 * * *}",
            zone = "${app.shipping.auto-deliver-zone:Europe/Istanbul}"
    )
    @Transactional
    public void autoMarkDelivered() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(autoDeliverDays);

        List<Order> overdueShipments = orderRepository
                .findByStatusIgnoreCaseAndShippedAtBefore("shipped", cutoff);

        if (overdueShipments.isEmpty()) {
            log.debug("AutoDelivery: Otomatik teslim edilecek sipariş bulunamadı.");
            return;
        }

        log.info("AutoDelivery: {} sipariş otomatik olarak 'delivered' yapılıyor (cutoff: {}).",
                overdueShipments.size(), cutoff);

        int updatedCount = 0;
        for (Order order : overdueShipments) {
            try {
                order.setStatus("delivered");
                orderRepository.save(order);
                updatedCount++;

                // Müşteriye teslim edildi bildirimi gönder
                sendDeliveryNotification(order);

                log.info("AutoDelivery: Sipariş {} → delivered. ({})",
                        order.getInvoice(), order.getShippingCarrier());
            } catch (Exception e) {
                log.error("AutoDelivery: Sipariş {} güncellenirken hata: {}",
                        order.getInvoice(), e.getMessage());
            }
        }

        log.info("AutoDelivery: Tamamlandı. {} / {} sipariş güncellendi.",
                updatedCount, overdueShipments.size());
    }

    private void sendDeliveryNotification(Order order) {
        try {
            String to = order.getEmail() != null ? order.getEmail() : order.getGuestEmail();
            if (to == null || to.isBlank()) return;

            // Mevcut EmailService üzerinden delivery maili
            emailService.sendDeliveryConfirmation(order);
        } catch (Exception e) {
            log.warn("AutoDelivery: Teslim bildirimi gönderilemedi (sipariş: {}): {}",
                    order.getInvoice(), e.getMessage());
        }
    }
}
