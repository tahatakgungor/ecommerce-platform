package com.ecommerce.product.api.admin;

import com.ecommerce.product.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/cloudinary")
@PreAuthorize("hasAnyAuthority('Admin','Staff')")
public class CloudinaryController {

    @PostMapping("/add-img")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("image") MultipartFile file) {
        log.info("Dosya yükleme isteği alındı: {}", file.getOriginalFilename());

        // Canlıya çıkışta buraya CloudinaryService gelecek.
        // Şimdilik Frontend'i kırmamak için sabit yapı:
        Map<String, String> response = Map.of(
                "url", "https://placehold.co/600x400?text=Yuklenen+Resim",
                "id", "temp/" + UUID.randomUUID()
        );

        return new ApiResponse<>(true, response, 1L);
    }

    @PostMapping("/add-multiple-img")
    public ApiResponse<List<Map<String, String>>> uploadMultipleImages(@RequestParam("image") MultipartFile[] files) {
        log.info("Çoklu dosya yükleme isteği alındı: {} dosya", files.length);

        List<Map<String, String>> response = java.util.Arrays.stream(files)
                .map(file -> Map.of(
                        "url", "https://placehold.co/600x400?text=Yuklenen+Resim",
                        "id", "temp/" + UUID.randomUUID()
                ))
                .toList();

        return new ApiResponse<>(true, response, (long) response.size());
    }

    @DeleteMapping("/img-delete")
    public ApiResponse<Map<String, String>> deleteImage(
            @RequestParam("folder_name") String folderName,
            @RequestParam("id") String id) {
        log.info("Dosya silme isteği alındı: {}/{}", folderName, id);

        return new ApiResponse<>(true, Map.of("result", "ok"), 1L);
    }
}
