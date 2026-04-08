package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.ActivityLogService;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/activity-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('Admin','Staff')")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    public ApiResponse<Map<String, Object>> getRecentLogs(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String eventType
    ) {
        Map<String, Object> result = activityLogService.getRecentLogs(limit, eventType);
        Object totalRaw = result.get("total");
        Long total = totalRaw instanceof Number ? ((Number) totalRaw).longValue() : 0L;
        return ApiResponse.ok(result, total);
    }
}
