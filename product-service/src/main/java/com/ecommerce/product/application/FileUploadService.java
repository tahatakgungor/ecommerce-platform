package com.ecommerce.product.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileUploadService {

    // .env veya Railway Variables'dan APP_BASE_URL'i okur. Bulamazsa localhost:8081 kullanır.
    @Value("${APP_BASE_URL:http://localhost:8081}")
    private String baseUrl;

    private final Path root = Paths.get("uploads");

    public String saveFile(MultipartFile file) {
        try {
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }

            String filename = UUID.randomUUID().toString().substring(0, 8) + "_" +
                    file.getOriginalFilename().replace(" ", "_"); // Boşlukları temizle

            Files.copy(file.getInputStream(), this.root.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

            // Hem lokalde hem canlıda tam URL döner: http://.../uploads/abc.jpg
            return baseUrl + "/uploads/" + filename;
        } catch (Exception e) {
            throw new RuntimeException("Dosya yükleme hatası: " + e.getMessage());
        }
    }
}