package com.ecommerce.product.api.client;

import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/coupon")
public class CouponClientController {

    // Harri hata almasın diye şimdilik boş bir liste dönüyoruz
    @GetMapping
    public List<?> getCoupons() {
        return new ArrayList<>();
    }
}