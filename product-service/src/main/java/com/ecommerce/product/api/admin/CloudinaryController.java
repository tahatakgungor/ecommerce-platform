package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.FileUploadService;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cloudinary")
@PreAuthorize("hasAnyAuthority('Admin','Staff')")
@RequiredArgsConstructor
public class CloudinaryController {

    private final FileUploadService fileUploadService;

    @PostMapping("/add-img")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("image") MultipartFile file) {
        log.info("Dosya yükleme isteği alındı: {}", file.getOriginalFilename());

        String path = fileUploadService.saveFile(file);
        String absoluteUrl = toAbsoluteUrl(path);
        Map<String, String> response = Map.of(
                "url", absoluteUrl,
                "id", path
        );

        return new ApiResponse<>(true, response, 1L);
    }

    @PostMapping("/add-multiple-img")
    public ApiResponse<List<Map<String, String>>> uploadMultipleImages(@RequestParam("image") MultipartFile[] files) {
        log.info("Çoklu dosya yükleme isteği alındı: {} dosya", files.length);

        List<Map<String, String>> response = java.util.Arrays.stream(files)
                .map(file -> {
                    String path = fileUploadService.saveFile(file);
                    return Map.of(
                            "url", toAbsoluteUrl(path),
                            "id", path
                    );
                })
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

    private String toAbsoluteUrl(String path) {
        if (path == null || path.isBlank()) return path;
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(path.startsWith("/") ? path : "/" + path)
                .toUriString();
    }
}
