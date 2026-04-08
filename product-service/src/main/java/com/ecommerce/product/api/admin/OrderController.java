package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.OrderService;
import com.ecommerce.product.application.SecurityRateLimitService;
import com.ecommerce.product.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final SecurityRateLimitService rateLimitService;

    @Value("${app.security.payment.init-rate-limit.max-attempts:20}")
    private int initPaymentMaxAttempts;

    @Value("${app.security.payment.init-rate-limit.window-minutes:10}")
    private long initPaymentWindowMinutes;

    @Value("${app.security.payment.confirm-rate-limit.max-attempts:25}")
    private int confirmPaymentMaxAttempts;

    @Value("${app.security.payment.confirm-rate-limit.window-minutes:10}")
    private long confirmPaymentWindowMinutes;

    @Value("${app.security.order-lookup-rate-limit.max-attempts:12}")
    private int orderLookupMaxAttempts;

    @Value("${app.security.order-lookup-rate-limit.window-minutes:15}")
    private long orderLookupWindowMinutes;

    // Admin: tüm siparişleri listele
    @GetMapping("/orders")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ResponseEntity<?> getAllOrders() {
        Map<String, Object> result = orderService.getAllOrders();
        return ResponseEntity.ok(new ApiResponse<>(true, result, (Long) result.get("total")));
    }

    // iyzico: ödeme formu başlat
    @PostMapping("/initialize-payment")
    public ResponseEntity<?> initializePayment(
            @RequestBody Map<String, Object> body,
            Authentication auth,
            HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        String fingerprint = buildClientFingerprint(request);
        String limitKey = "payment-init:" + clientIp + ":" + fingerprint;
        Duration window = Duration.ofMinutes(initPaymentWindowMinutes);
        rateLimitService.assertAllowed(
                limitKey,
                initPaymentMaxAttempts,
                window,
                "RATE_LIMIT_EXCEEDED:PAYMENT_INIT"
        );

        String email = (auth != null) ? auth.getName() : "anonymousUser";
        try {
            Map<String, Object> result = orderService.initializePayment(body, email, request);
            rateLimitService.clearFailures(limitKey);
            return ResponseEntity.ok(result);
        } catch (RuntimeException ex) {
            rateLimitService.registerFailure(limitKey, window);
            throw ex;
        }
    }

    // iyzico: ödeme doğrula ve siparişi kaydet
    @PostMapping("/confirm-payment")
    public ResponseEntity<?> confirmPayment(
            @RequestBody Map<String, Object> body,
            Authentication auth,
            HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        String fingerprint = buildClientFingerprint(request);
        String token = body.get("token") != null ? body.get("token").toString() : "no-token";
        String conversationId = body.get("conversationId") != null ? body.get("conversationId").toString() : "no-conversation";
        String limitKey = "payment-confirm:" + clientIp + ":" + fingerprint + ":" + token + ":" + conversationId;
        Duration window = Duration.ofMinutes(confirmPaymentWindowMinutes);
        rateLimitService.assertAllowed(
                limitKey,
                confirmPaymentMaxAttempts,
                window,
                "RATE_LIMIT_EXCEEDED:PAYMENT_CONFIRM"
        );

        String email = (auth != null) ? auth.getName() : "anonymousUser";
        try {
            Map<String, Object> result = orderService.confirmPayment(body, email);
            rateLimitService.clearFailures(limitKey);
            return ResponseEntity.ok(result);
        } catch (RuntimeException ex) {
            rateLimitService.registerFailure(limitKey, window);
            throw ex;
        }
    }

    // Admin: sipariş durumu güncelle
    @PatchMapping("/update-order/{id}")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        Map<String, Object> result = orderService.updateOrderStatus(id, body.get("status"));
        return ResponseEntity.ok(new ApiResponse<>(true, result, null));
    }

    // Admin: kargo bilgisi güncelle
    @PatchMapping("/update-shipping/{id}")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ResponseEntity<?> updateShippingInfo(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        Map<String, Object> result = orderService.updateShippingInfo(id, body.get("carrier"), body.get("trackingNumber"));
        return ResponseEntity.ok(new ApiResponse<>(true, result, null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ResponseEntity<?> getOrderById(@PathVariable UUID id) {
        Map<String, Object> result = orderService.getSingleOrder(id);
        return ResponseEntity.ok(new ApiResponse<>(true, result, null));
    }

    // Misafir sipariş sorgulama
    @GetMapping("/lookup")
    public ResponseEntity<?> lookupOrder(
            @RequestParam String invoice,
            @RequestParam String email,
            HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        String normalizedEmail = normalize(email);
        String limitKey = "order-lookup:" + clientIp + ":" + invoice + ":" + normalizedEmail;
        Duration window = Duration.ofMinutes(orderLookupWindowMinutes);
        rateLimitService.assertAllowed(
                limitKey,
                orderLookupMaxAttempts,
                window,
                "RATE_LIMIT_EXCEEDED:ORDER_LOOKUP"
        );

        try {
            Map<String, Object> result = orderService.getOrderByInvoiceAndEmail(invoice, email);
            rateLimitService.clearFailures(limitKey);
            return ResponseEntity.ok(new ApiResponse<>(true, result, null));
        } catch (RuntimeException ex) {
            rateLimitService.registerFailure(limitKey, window);
            log.warn("[Security][OrderLookup] invoice={} email={} ip={} message={}",
                    invoice, normalizedEmail, clientIp, ex.getMessage());
            throw new RuntimeException("Sipariş bulunamadı.");
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String buildClientFingerprint(HttpServletRequest request) {
        String ua = valueOrUnknown(request.getHeader("User-Agent"));
        String lang = valueOrUnknown(request.getHeader("Accept-Language"));
        String platform = valueOrUnknown(request.getHeader("sec-ch-ua-platform"));
        return Integer.toHexString((ua + "|" + lang + "|" + platform).hashCode());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String valueOrUnknown(String value) {
        if (value == null || value.isBlank()) return "unknown";
        return value.trim();
    }
}
