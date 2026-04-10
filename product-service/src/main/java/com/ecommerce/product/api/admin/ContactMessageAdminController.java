package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.ContactMessageService;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/contact-messages")
@RequiredArgsConstructor
public class ContactMessageAdminController {

    private final ContactMessageService contactMessageService;

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        Map<String, Object> result = contactMessageService.getAllForAdmin(status, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, result, null));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "admin";
        String status = body.get("status");
        String adminNote = body.get("adminNote");
        Map<String, Object> result = contactMessageService.updateStatus(id, status, adminNote, actor);
        return ResponseEntity.ok(new ApiResponse<>(true, result, null));
    }
}
