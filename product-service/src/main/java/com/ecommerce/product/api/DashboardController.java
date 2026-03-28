package com.ecommerce.product.api;

import com.ecommerce.product.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-order")
@CrossOrigin(origins = "http://localhost:3000")
public class DashboardController {

    // Dashboard'daki üst kartlar (Toplam Kazanç, Sipariş Sayısı vb.)
    @GetMapping("/dashboard-amount")
    public ApiResponse<Object> getDashboardAmount() {
        // Harri genellikle bir liste içinde anahtar-değer çiftleri bekler
        Map<String, Object> stats = Map.of(
                "totalOrder", 0,
                "pendingOrder", 0,
                "totalAmount", 0.0,
                "todayRevenue", 0.0
        );
        return new ApiResponse<>(true, List.of(stats), 1L);
    }

    // Son Siparişler Tablosu
    @GetMapping("/dashboard-recent-order")
    public ApiResponse<List<?>> getRecentOrders() {
        // Şu an veri olmadığı için boş liste dönüyoruz ama yapı Harri'ye uygun
        return new ApiResponse<>(true, List.of(), 0L);
    }
}