package com.ecommerce.product.application;

import com.ecommerce.product.domain.ContactMessage;
import com.ecommerce.product.dto.ContactRequest;
import com.ecommerce.product.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public Map<String, Object> create(ContactRequest request) {
        ContactMessage message = new ContactMessage();
        message.setName(trimToNull(request.getName()));
        message.setEmail(normalizeEmail(request.getEmail()));
        message.setPhone(trimToNull(request.getPhone()));
        message.setCompany(trimToNull(request.getCompany()));
        message.setMessage(trimToNull(request.getMessage()));
        message.setStatus("NEW");

        ContactMessage saved = contactMessageRepository.save(message);

        activityLogService.log(
                "CONTACT_MESSAGE_CREATED",
                "INFO",
                "Yeni iletişim formu mesajı alındı.",
                saved.getEmail(),
                "CONTACT_MESSAGE",
                saved.getId().toString(),
                Map.of("email", saved.getEmail())
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public Map<String, Object> getAllForAdmin(String status, Integer page, Integer size) {
        int safePage = page != null && page >= 0 ? page : 1;
        int safeSize = size != null && size > 0 && size <= 200 ? size : 50;
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ContactMessage> data;
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            data = contactMessageRepository.findAll(pageable);
        } else {
            data = contactMessageRepository.findByStatusIgnoreCaseOrderByCreatedAtDesc(status.trim(), pageable);
        }

        List<Map<String, Object>> messages = data.getContent().stream()
                .map(this::toResponse)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("messages", messages);
        result.put("total", data.getTotalElements());
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public Map<String, Object> updateStatus(UUID id, String nextStatus, String adminNote, String actor) {
        ContactMessage item = contactMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("İletişim mesajı bulunamadı."));

        String normalized = normalizeStatus(nextStatus);
        item.setStatus(normalized);
        item.setAdminNote(trimToNull(adminNote));
        item.setHandledBy(trimToNull(actor));

        ContactMessage saved = contactMessageRepository.save(item);

        activityLogService.log(
                "CONTACT_MESSAGE_STATUS_UPDATED",
                "INFO",
                "İletişim mesajı durumu güncellendi.",
                actor != null ? actor : "admin",
                "CONTACT_MESSAGE",
                saved.getId().toString(),
                Map.of("status", normalized)
        );

        return toResponse(saved);
    }

    private String normalizeStatus(String status) {
        String value = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "NEW", "IN_PROGRESS", "RESOLVED" -> value;
            default -> throw new RuntimeException("Geçersiz durum. NEW, IN_PROGRESS veya RESOLVED olmalı.");
        };
    }

    private Map<String, Object> toResponse(ContactMessage item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("_id", item.getId().toString());
        result.put("name", item.getName());
        result.put("email", item.getEmail());
        result.put("phone", item.getPhone());
        result.put("company", item.getCompany());
        result.put("message", item.getMessage());
        result.put("status", item.getStatus());
        result.put("adminNote", item.getAdminNote());
        result.put("handledBy", item.getHandledBy());
        result.put("createdAt", item.getCreatedAt() != null ? item.getCreatedAt().toString() : null);
        result.put("updatedAt", item.getUpdatedAt() != null ? item.getUpdatedAt().toString() : null);
        return result;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeEmail(String value) {
        if (value == null) return null;
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
