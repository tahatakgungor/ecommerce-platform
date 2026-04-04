package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.OrderService;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Admin: tüm siparişleri listele
    @GetMapping("/orders")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ResponseEntity<?> getAllOrders() {
        Map<String, Object> result = orderService.getAllOrders();
        return ResponseEntity.ok(new ApiResponse<>(true, result, (Long) result.get("total")));
    }

    // Müşteri: sipariş oluştur (ödeme sonrası)
    @PostMapping("/addOrder")
    public ResponseEntity<?> addOrder(@RequestBody Map<String, Object> body, Authentication auth) {
        Map<String, Object> result = orderService.addOrder(body, auth.getName());
        return ResponseEntity.ok(result);
    }

    // Stripe: payment intent oluştur
    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody Map<String, Object> body) {
        int price = Integer.parseInt(body.get("price").toString());
        Map<String, String> result = orderService.createPaymentIntent(price);
        return ResponseEntity.ok(result);
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

    // Admin: tekil sipariş
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ResponseEntity<?> getOrderById(@PathVariable UUID id) {
        Map<String, Object> result = orderService.getSingleOrder(id);
        return ResponseEntity.ok(new ApiResponse<>(true, result, null));
    }
}
