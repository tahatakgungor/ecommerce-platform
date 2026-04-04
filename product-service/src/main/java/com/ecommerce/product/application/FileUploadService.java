package com.ecommerce.product.application;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileUploadService {

    private final Path root = Paths.get("uploads");

    public String saveFile(MultipartFile file) {
        try {
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }

            String filename = buildSafeFilename(file.getOriginalFilename());

            Files.copy(file.getInputStream(), this.root.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + filename;
        } catch (Exception e) {
            throw new RuntimeException("Dosya yükleme hatası: " + e.getMessage());
        }
    }

    private String buildSafeFilename(String originalFilename) {
        String extension = ".bin";
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < originalFilename.length() - 1) {
                String candidate = originalFilename.substring(dotIndex + 1).toLowerCase();
                candidate = candidate.replaceAll("[^a-z0-9]", "");
                if (!candidate.isBlank() && candidate.length() <= 8) {
                    extension = "." + candidate;
                }
            }
        }
        return UUID.randomUUID().toString().replace("-", "") + extension;
    }
}
