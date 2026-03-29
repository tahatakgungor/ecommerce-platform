package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.FileUploadService;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('Admin')")
    public ApiResponse<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = fileUploadService.saveFile(file);
        return ApiResponse.ok(fileUrl, 1L);
    }
}