package com.ecommerce.product.application;

import com.ecommerce.product.domain.ActivityLog;
import com.ecommerce.product.repository.ActivityLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper;

    public void log(String eventType,
                    String severity,
                    String message,
                    String actor,
                    String targetType,
                    String targetId,
                    Map<String, Object> metadata) {
        try {
            ActivityLog activityLog = new ActivityLog();
            activityLog.setEventType(safe(eventType, "UNKNOWN_EVENT"));
            activityLog.setSeverity(safe(severity, "INFO"));
            activityLog.setMessage(safe(message, "No message"));
            activityLog.setActor(trimOrNull(actor));
            activityLog.setTargetType(trimOrNull(targetType));
            activityLog.setTargetId(trimOrNull(targetId));
            activityLog.setMetadata(toJson(metadata));
            activityLogRepository.save(activityLog);
        } catch (Exception e) {
            log.warn("Activity log save failed: {}", e.getMessage());
        }
    }

    public Map<String, Object> getRecentLogs(Integer limit, String eventType) {
        int safeLimit = limit == null ? 50 : Math.max(1, Math.min(limit, 200));

        List<ActivityLog> logs = (eventType != null && !eventType.isBlank())
                ? activityLogRepository.findTop200ByEventTypeOrderByCreatedAtDesc(eventType.trim())
                : activityLogRepository.findTop200ByOrderByCreatedAtDesc();

        List<Map<String, Object>> sliced = logs.stream()
                .limit(safeLimit)
                .map(this::toResponse)
                .toList();

        return Map.of(
                "logs", sliced,
                "total", sliced.size()
        );
    }

    private Map<String, Object> toResponse(ActivityLog log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId() != null ? log.getId().toString() : null);
        map.put("eventType", log.getEventType());
        map.put("severity", log.getSeverity());
        map.put("message", log.getMessage());
        map.put("actor", log.getActor());
        map.put("targetType", log.getTargetType());
        map.put("targetId", log.getTargetId());
        map.put("metadata", fromJson(log.getMetadata()));
        map.put("createdAt", log.getCreatedAt() != null ? log.getCreatedAt().toString() : null);
        return map;
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            return null;
        }
    }

    private Object fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Object>() {});
        } catch (Exception e) {
            return json;
        }
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
