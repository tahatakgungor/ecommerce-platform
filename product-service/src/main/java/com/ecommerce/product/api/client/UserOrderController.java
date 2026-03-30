package com.ecommerce.product.api.client;

import com.ecommerce.product.application.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user-order")
@RequiredArgsConstructor
public class UserOrderController {

    private final OrderService orderService;

    // Giriş yapmış kullanıcının siparişleri
    @GetMapping("/order-by-user")
    public ResponseEntity<?> getOrdersByUser(Authentication auth) {
        Map<String, Object> result = orderService.getOrdersByUser(auth.getName());
        return ResponseEntity.ok(result);
    }

    // Tek sipariş detayı (fatura sayfası)
    @GetMapping("/single-order/{id}")
    public ResponseEntity<?> getSingleOrder(@PathVariable UUID id) {
        Map<String, Object> result = orderService.getSingleOrder(id);
        return ResponseEntity.ok(result);
    }
}
