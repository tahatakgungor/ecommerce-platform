package com.ecommerce.product.api.client;

import com.ecommerce.product.application.OrderService;
import com.ecommerce.product.application.OrderReturnService;
import com.ecommerce.product.dto.returns.CreateReturnRequest;
import jakarta.validation.Valid;
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
    private final OrderReturnService orderReturnService;

    // Giriş yapmış kullanıcının siparişleri
    @GetMapping("/order-by-user")
    public ResponseEntity<?> getOrdersByUser(Authentication auth) {
        Map<String, Object> result = orderService.getOrdersByUser(auth.getName());
        return ResponseEntity.ok(result);
    }

    // Tek sipariş detayı (fatura sayfası)
    @GetMapping("/single-order/{id}")
    public ResponseEntity<?> getSingleOrder(@PathVariable UUID id, Authentication auth) {
        String email = (auth != null) ? auth.getName() : null;
        Map<String, Object> result = orderService.getSingleOrderForUser(email, id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{orderId}/returns")
    public ResponseEntity<?> createReturn(
            @PathVariable UUID orderId,
            @Valid @RequestBody CreateReturnRequest request,
            Authentication auth) {
        String email = (auth != null) ? auth.getName() : null;
        Map<String, Object> result = orderReturnService.createReturn(email, orderId, request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/returns")
    public ResponseEntity<?> getMyReturns(Authentication auth) {
        String email = (auth != null) ? auth.getName() : null;
        Map<String, Object> result = orderReturnService.getMyReturns(email);
        return ResponseEntity.ok(result);
    }
}
