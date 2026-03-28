package com.ecommerce.product.api;

import com.ecommerce.product.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cloudinary")
@CrossOrigin(origins = "http://localhost:3000")
public class CloudinaryController {

    @PostMapping("/add-img")
    public ApiResponse<Map<String, Object>> uploadImage(@RequestParam("image") MultipartFile file) {
        // Harri'nin beklediği iç yapı (url ve id)
        Map<String, Object> imageData = new HashMap<>();

        // Şimdilik dummy bir URL ve rastgele bir ID veriyoruz
        imageData.put("url", "https://via.placeholder.com/600x400?text=Product+Image");
        imageData.put("id", "dummy_id_" + System.currentTimeMillis());

        // ÖNEMLİ: ApiResponse ile sarmalıyoruz ki Frontend 'data.url' diyebilsin
        return new ApiResponse<>(true, imageData, 1L);
    }
}