package com.ecommerce.product.api;

import com.ecommerce.product.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/order")
@CrossOrigin(origins = {"http://localhost:3000", "https://ecommerce-frontend-xryc.vercel.app"})
public class OrderController {

    @GetMapping("/orders")
    public ApiResponse<List<?>> getOrders() {
        // Tablo boş bile olsa ApiResponse formatında sarmalanmış olmalı
        return new ApiResponse<>(true, List.of(), 0L);
    }

    // Harri bazen tekil sipariş detayına da bakar (opsiyonel ama dursun)
    @GetMapping("/{id}")
    public ApiResponse<Object> getOrderById(@PathVariable String id) {
        return new ApiResponse<>(false, null, 0L); // Sipariş sistemi henüz yok
    }
}