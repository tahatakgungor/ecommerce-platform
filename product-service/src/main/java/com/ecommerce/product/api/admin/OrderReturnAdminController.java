package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.OrderReturnService;
import com.ecommerce.product.dto.returns.UpdateReturnStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/returns")
@RequiredArgsConstructor
public class OrderReturnAdminController {

    private final OrderReturnService orderReturnService;

    @GetMapping
    public ResponseEntity<?> getAllReturns() {
        Map<String, Object> result = orderReturnService.getAllReturnsForAdmin();
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateReturnStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReturnStatusRequest request,
            Authentication auth) {
        String actor = auth != null ? auth.getName() : "system";
        Map<String, Object> result = orderReturnService.updateStatus(id, request, actor);
        return ResponseEntity.ok(result);
    }
}
