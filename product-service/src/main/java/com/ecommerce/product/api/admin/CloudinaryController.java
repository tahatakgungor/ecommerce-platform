package com.ecommerce.product.api.admin;

import com.ecommerce.product.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/cloudinary")
public class CloudinaryController {

    @PostMapping("/add-img")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("image") MultipartFile file) {
        log.info("Dosya yükleme isteği alındı: {}", file.getOriginalFilename());

        // Canlıya çıkışta buraya CloudinaryService gelecek.
        // Şimdilik Frontend'i kırmamak için sabit yapı:
        Map<String, String> response = Map.of(
                "url", "https://placehold.co/600x400?text=Yuklenen+Resim",
                "id", "temp_" + UUID.randomUUID().toString()
        );

        return new ApiResponse<>(true, response, 1L);
    }
}