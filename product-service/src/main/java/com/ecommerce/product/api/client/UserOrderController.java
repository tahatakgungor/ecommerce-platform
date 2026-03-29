package com.ecommerce.product.api.client;

import com.ecommerce.product.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-order")
public class UserOrderController {

    @GetMapping("/order-by-user")
    public ApiResponse<List<?>> getOrdersByUser() {
        return new ApiResponse<>(true, List.of(), 0L);
    }

    @GetMapping("/single-order/{id}")
    public ApiResponse<Object> getSingleOrder(@PathVariable String id) {
        return new ApiResponse<>(true, null, 0L);
    }
}
