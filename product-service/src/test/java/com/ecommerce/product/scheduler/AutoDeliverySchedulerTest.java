package com.ecommerce.product.scheduler;

import com.ecommerce.product.application.EmailService;
import com.ecommerce.product.application.ActivityLogService;
import com.ecommerce.product.domain.Order;
import com.ecommerce.product.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoDeliverySchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private ActivityLogService activityLogService;

    private AutoDeliveryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AutoDeliveryScheduler(orderRepository, emailService, activityLogService);
        ReflectionTestUtils.setField(scheduler, "autoDeliverDays", 10);
    }

    @Test
    void autoMarkDelivered_shouldSkipWhenNoOverdueShipment() {
        when(orderRepository.findByStatusIgnoreCaseAndShippedAtBefore(eq("shipped"), any(LocalDateTime.class)))
                .thenReturn(List.of());

        scheduler.autoMarkDelivered();

        verify(orderRepository, never()).save(any(Order.class));
        verify(emailService, never()).sendDeliveryConfirmation(any(Order.class));
    }

    @Test
    void autoMarkDelivered_shouldSetDeliveredAndSendNotificationForOverdueShipments() {
        Order firstOrder = new Order();
        firstOrder.setStatus("shipped");
        firstOrder.setEmail("first@example.com");

        Order secondOrder = new Order();
        secondOrder.setStatus("shipped");
        secondOrder.setGuestEmail("second@example.com");

        when(orderRepository.findByStatusIgnoreCaseAndShippedAtBefore(eq("shipped"), any(LocalDateTime.class)))
                .thenReturn(List.of(firstOrder, secondOrder));

        scheduler.autoMarkDelivered();

        assertEquals("delivered", firstOrder.getStatus());
        assertEquals("delivered", secondOrder.getStatus());
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(emailService, times(2)).sendDeliveryConfirmation(any(Order.class));
    }
}
