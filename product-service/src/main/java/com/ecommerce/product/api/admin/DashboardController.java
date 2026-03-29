package com.ecommerce.product.api.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-order")
@Slf4j
public class DashboardController {

    /**
     * Dashboard Kart Verileri
     * KÖKTEN ÇÖZÜM: ApiResponse sarmalayıcısını kaldırıyoruz.
     * Frontend direkt bu objeyi 'dashboardOrderAmount' olarak alacak.
     */
    @GetMapping("/dashboard-amount")
    public Map<String, Object> getDashboardAmount() {
        log.info("Dashboard verileri RAW formatta gönderiliyor...");

        // Frontend'in card-items.tsx içinde beklediği TAM yapı:
        return Map.of(
                "totalOrder", 0,
                "pendingOrder", 0,
                "totalOrderAmount", 0.0, // toFixed(2) için sayısal değer
                "todayRevenue", 0.0
        );
    }

    // Diğer endpoint'leri de sarmalayıcı olmadan (raw) dönelim ki onlar da patlamasın
    @GetMapping("/most-selling-category")
    public List<Object> getMostSellingCategory() {
        return List.of();
    }

    @GetMapping("/sales-report")
    public List<Object> getSalesReport() {
        return List.of();
    }

    @GetMapping("/dashboard-recent-order")
    public List<Object> getRecentOrders() {
        return List.of();
    }
}