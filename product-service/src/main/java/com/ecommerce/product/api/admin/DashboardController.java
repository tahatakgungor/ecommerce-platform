package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user-order")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final OrderService orderService;

    /**
     * Dashboard kart verileri:
     * { todayOrderAmount, yesterdayOrderAmount, monthlyOrderAmount, totalOrderAmount,
     *   todayCardPaymentAmount, todayCashPaymentAmount,
     *   yesterDayCardPaymentAmount, yesterDayCashPaymentAmount }
     */
    @GetMapping("/dashboard-amount")
    public Map<String, Object> getDashboardAmount() {
        log.info("Dashboard amount isteği alındı");
        return orderService.getDashboardAmount();
    }

    /**
     * Son siparişler:
     * { orders: [...], totalOrder: N }
     */
    @GetMapping("/dashboard-recent-order")
    public Map<String, Object> getRecentOrders() {
        log.info("Dashboard recent orders isteği alındı");
        return orderService.getDashboardRecentOrders();
    }

    /**
     * Satış raporu (son 30 gün, güne göre gruplu):
     * { salesReport: [{ date, total, order }] }
     */
    @GetMapping("/sales-report")
    public Map<String, Object> getSalesReport() {
        log.info("Dashboard sales report isteği alındı");
        return orderService.getSalesReport();
    }

    /**
     * En çok satan kategori:
     * { categoryData: [{ _id, count }] }
     */
    @GetMapping("/most-selling-category")
    public Map<String, Object> getMostSellingCategory() {
        log.info("Dashboard most selling category isteği alındı");
        return orderService.getMostSellingCategory();
    }
}
