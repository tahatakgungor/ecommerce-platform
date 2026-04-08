package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.OrderService;
import com.ecommerce.product.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
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

    // iyzico: ödeme formu başlat
    @PostMapping("/initialize-payment")
    public ResponseEntity<?> initializePayment(
            @RequestBody Map<String, Object> body,
            Authentication auth,
            HttpServletRequest request) {
        String email = (auth != null) ? auth.getName() : "anonymousUser";
        Map<String, Object> result = orderService.initializePayment(body, email, request);
        return ResponseEntity.ok(result);
    }

    // iyzico: ödeme doğrula ve siparişi kaydet
    @PostMapping("/confirm-payment")
    public ResponseEntity<?> confirmPayment(@RequestBody Map<String, Object> body, Authentication auth) {
        String email = (auth != null) ? auth.getName() : "anonymousUser";
        Map<String, Object> result = orderService.confirmPayment(body, email);
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ResponseEntity<?> getOrderById(@PathVariable UUID id) {
        Map<String, Object> result = orderService.getSingleOrder(id);
        return ResponseEntity.ok(new ApiResponse<>(true, result, null));
    }

    // Misafir sipariş sorgulama
    @GetMapping("/lookup")
    public ResponseEntity<?> lookupOrder(
            @RequestParam int invoice,
            @RequestParam String email) {
        Map<String, Object> result = orderService.getOrderByInvoiceAndEmail(invoice, email);
        return ResponseEntity.ok(new ApiResponse<>(true, result, null));
    }
}
