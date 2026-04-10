package com.ecommerce.product.application;

import com.ecommerce.product.domain.Order;
import com.ecommerce.product.domain.OrderReturn;
import com.ecommerce.product.domain.OrderReturnStatus;
import com.ecommerce.product.domain.User;
import com.ecommerce.product.dto.returns.CreateReturnRequest;
import com.ecommerce.product.dto.returns.UpdateReturnStatusRequest;
import com.ecommerce.product.event.OrderReturnCreatedEvent;
import com.ecommerce.product.event.OrderReturnStatusChangedEvent;
import com.ecommerce.product.repository.OrderRepository;
import com.ecommerce.product.repository.OrderReturnRepository;
import com.ecommerce.product.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderReturnService {

    private final OrderReturnRepository orderReturnRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SiteSettingsService siteSettingsService;
    private final ActivityLogService activityLogService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Map<String, Object> createReturn(String userEmail, UUID orderId, CreateReturnRequest request) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new RuntimeException("İade talebi için giriş yapmanız gerekiyor.");
        }

        User user = userRepository.findByEmail(userEmail.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı."));

        assertOwnership(user.getEmail(), user.getId(), order);
        assertOrderEligibleForReturn(order);

        if (orderReturnRepository.existsByOrderIdAndStatusIn(
                orderId,
                List.of(OrderReturnStatus.REQUESTED, OrderReturnStatus.APPROVED, OrderReturnStatus.RECEIVED)
        )) {
            throw new RuntimeException("Bu sipariş için açık bir iade talebi zaten bulunuyor.");
        }

        OrderReturn orderReturn = new OrderReturn();
        orderReturn.setOrderId(orderId);
        orderReturn.setUserId(user.getId() != null ? user.getId().toString() : null);
        orderReturn.setUserEmail(user.getEmail());
        orderReturn.setGuestOrder(Boolean.TRUE.equals(order.getIsGuest()));
        orderReturn.setReason(normalizeRequired(request.getReason(), "İade nedeni zorunludur."));
        orderReturn.setCustomerNote(normalizeNullable(request.getCustomerNote()));
        orderReturn.setStatus(OrderReturnStatus.REQUESTED);
        orderReturn.setStatusHistory(toJson(List.of(buildHistoryEntry(OrderReturnStatus.REQUESTED, "Talep oluşturuldu.", user.getEmail()))));

        OrderReturn saved = orderReturnRepository.save(orderReturn);

        // Denormalize returnStatus onto the Order for fast admin lookup
        order.setReturnStatus(OrderReturnStatus.REQUESTED.name());
        orderRepository.save(order);

        activityLogService.log(
                "ORDER_RETURN_CREATED",
                "INFO",
                "Sipariş iade talebi oluşturuldu.",
                user.getEmail(),
                "ORDER_RETURN",
                saved.getId().toString(),
                Map.of("orderId", orderId.toString(), "status", saved.getStatus().name())
        );

        try {
            eventPublisher.publishEvent(new OrderReturnCreatedEvent(saved, order));
        } catch (Exception e) {
            // Email hatası işlemi geri sarmamalı
        }

        return Map.of("return", toResponse(saved));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyReturns(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new RuntimeException("İade talepleri için giriş yapmanız gerekiyor.");
        }
        List<Map<String, Object>> items = orderReturnRepository.findByUserEmailIgnoreCaseOrderByCreatedAtDesc(userEmail.trim())
                .stream()
                .map(this::toResponse)
                .toList();
        return Map.of("returns", items, "total", (long) items.size());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public Map<String, Object> getAllReturnsForAdmin() {
        List<Map<String, Object>> items = orderReturnRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
        return Map.of("returns", items, "total", (long) items.size());
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public Map<String, Object> updateStatus(UUID returnId, UpdateReturnStatusRequest request, String actorEmail) {
        OrderReturn orderReturn = orderReturnRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("İade talebi bulunamadı."));

        OrderReturnStatus nextStatus = request.getStatus();
        validateTransition(orderReturn.getStatus(), nextStatus);

        orderReturn.setStatus(nextStatus);
        orderReturn.setAdminNote(normalizeNullable(request.getAdminNote()));
        orderReturn.setProcessedBy(actorEmail);

        List<Map<String, Object>> history = parseHistory(orderReturn.getStatusHistory());
        history.add(buildHistoryEntry(nextStatus, orderReturn.getAdminNote(), actorEmail));
        orderReturn.setStatusHistory(toJson(history));

        OrderReturn saved = orderReturnRepository.save(orderReturn);

        activityLogService.log(
                "ORDER_RETURN_STATUS_UPDATED",
                "INFO",
                "İade durumu güncellendi.",
                actorEmail,
                "ORDER_RETURN",
                saved.getId().toString(),
                Map.of("status", nextStatus.name())
        );

        // Sync returnStatus on the Order entity
        orderRepository.findById(saved.getOrderId()).ifPresent(order -> {
            order.setReturnStatus(nextStatus.name());
            orderRepository.save(order);
            try {
                eventPublisher.publishEvent(new OrderReturnStatusChangedEvent(saved, order));
            } catch (Exception e) {
                // Email hatası işlemi geri sarmamalı
            }
        });

        return Map.of("return", toResponse(saved));
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> findReturnByOrderId(UUID orderId) {
        return orderReturnRepository.findByOrderId(orderId).map(this::toResponse);
    }

    private void assertOwnership(String userEmail, UUID userId, Order order) {
        String normalizedUserEmail = safeLower(userEmail);
        String orderEmail = safeLower(order.getEmail());
        String guestEmail = safeLower(order.getGuestEmail());
        String orderUserId = order.getUserId() != null ? order.getUserId().trim() : "";

        boolean byEmail = normalizedUserEmail.equals(orderEmail) || normalizedUserEmail.equals(guestEmail);
        boolean byUserId = userId != null && userId.toString().equals(orderUserId);

        if (!byEmail && !byUserId) {
            throw new RuntimeException("Bu sipariş için iade talebi oluşturamazsınız.");
        }
    }

    private void assertOrderEligibleForReturn(Order order) {
        String status = safeLower(order.getStatus());
        if (!"delivered".equals(status)) {
            throw new RuntimeException("Sadece teslim edilmiş siparişler için iade talebi oluşturulabilir.");
        }
        if (order.getCreatedAt() == null) {
            return;
        }
        int returnWindowDays = Math.max(1, siteSettingsService.getReturnWindowDays());
        LocalDateTime cutoff = LocalDateTime.now(ZoneId.of("Europe/Istanbul")).minusDays(returnWindowDays);
        if (order.getCreatedAt().isBefore(cutoff)) {
            throw new RuntimeException("İade süresi dolmuş siparişler için talep oluşturulamaz.");
        }
    }

    private void validateTransition(OrderReturnStatus current, OrderReturnStatus next) {
        if (current == next) {
            return;
        }
        Map<OrderReturnStatus, Set<OrderReturnStatus>> transitions = Map.of(
                OrderReturnStatus.REQUESTED, EnumSet.of(OrderReturnStatus.APPROVED, OrderReturnStatus.REJECTED),
                OrderReturnStatus.APPROVED, EnumSet.of(OrderReturnStatus.RECEIVED),
                OrderReturnStatus.RECEIVED, EnumSet.of(OrderReturnStatus.REFUNDED),
                OrderReturnStatus.REJECTED, EnumSet.noneOf(OrderReturnStatus.class),
                OrderReturnStatus.REFUNDED, EnumSet.noneOf(OrderReturnStatus.class)
        );

        Set<OrderReturnStatus> allowed = transitions.getOrDefault(current, EnumSet.noneOf(OrderReturnStatus.class));
        if (!allowed.contains(next)) {
            throw new RuntimeException("Geçersiz iade durum geçişi: " + current + " -> " + next);
        }
    }

    private Map<String, Object> toResponse(Order item) {
        return Map.of(
                "_id", item.getId().toString(),
                "invoice", item.getInvoice(),
                "status", item.getStatus(),
                "totalAmount", item.getTotalAmount(),
                "createdAt", item.getCreatedAt() != null ? item.getCreatedAt().toString() : null
        );
    }

    private Map<String, Object> toResponse(OrderReturn item) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("_id", item.getId().toString());
        response.put("orderId", item.getOrderId().toString());
        response.put("userId", item.getUserId());
        response.put("userEmail", item.getUserEmail());
        response.put("guestOrder", item.isGuestOrder());
        response.put("reason", item.getReason());
        response.put("customerNote", item.getCustomerNote());
        response.put("status", item.getStatus().name());
        response.put("adminNote", item.getAdminNote());
        response.put("processedBy", item.getProcessedBy());
        response.put("createdAt", item.getCreatedAt() != null ? item.getCreatedAt().toString() : null);
        response.put("updatedAt", item.getUpdatedAt() != null ? item.getUpdatedAt().toString() : null);
        response.put("statusHistory", parseHistory(item.getStatusHistory()));

        orderRepository.findById(item.getOrderId()).ifPresent(order -> response.put("order", toResponse(order)));
        return response;
    }

    private List<Map<String, Object>> parseHistory(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Map<String, Object> buildHistoryEntry(OrderReturnStatus status, String note, String actor) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("status", status.name());
        entry.put("note", normalizeNullable(note));
        entry.put("actor", normalizeNullable(actor));
        entry.put("changedAt", LocalDateTime.now(ZoneId.of("Europe/Istanbul")).toString());
        return entry;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new RuntimeException(message);
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
