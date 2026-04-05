package com.ecommerce.product.application;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    private final Path root = Paths.get("uploads");
    private Cloudinary cloudinary;
    private boolean cloudinaryEnabled;

    @Value("${app.upload.provider:auto}")
    private String uploadProvider;

    @Value("${app.upload.cloudinary.cloud-name:}")
    private String cloudinaryCloudName;

    @Value("${app.upload.cloudinary.api-key:}")
    private String cloudinaryApiKey;

    @Value("${app.upload.cloudinary.api-secret:}")
    private String cloudinaryApiSecret;

    @Value("${app.upload.cloudinary.folder:serravit}")
    private String cloudinaryFolder;

    @PostConstruct
    public void init() {
        boolean hasCredentials = isNotBlank(cloudinaryCloudName)
                && isNotBlank(cloudinaryApiKey)
                && isNotBlank(cloudinaryApiSecret);

        String provider = normalize(uploadProvider);
        boolean forceCloudinary = "cloudinary".equals(provider);
        boolean forceLocal = "local".equals(provider);

        if (hasCredentials && !forceLocal) {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudinaryCloudName,
                    "api_key", cloudinaryApiKey,
                    "api_secret", cloudinaryApiSecret,
                    "secure", true
            ));
            this.cloudinaryEnabled = true;
            log.info("File upload provider: cloudinary (folder={})", cloudinaryFolder);
            return;
        }

        this.cloudinaryEnabled = false;
        if (forceCloudinary) {
            log.warn("UPLOAD_PROVIDER=cloudinary ama Cloudinary bilgileri eksik. Local uploads moduna düşüldü.");
        } else {
            log.warn("File upload provider: local filesystem. Bu mod production için kalıcı değildir.");
        }
    }

    public String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Yüklenecek dosya bulunamadı.");
        }

        if (cloudinaryEnabled) {
            return saveToCloudinary(file);
        }

        return saveToLocal(file);
    }

    private String saveToCloudinary(MultipartFile file) {
        try {
            String publicId = UUID.randomUUID().toString().replace("-", "");
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", cloudinaryFolder,
                            "public_id", publicId,
                            "resource_type", "auto",
                            "overwrite", true
                    )
            );

            String secureUrl = toStringOrNull(result.get("secure_url"));
            if (!isNotBlank(secureUrl)) {
                throw new RuntimeException("Cloudinary yanıtında secure_url yok.");
            }
            return secureUrl;
        } catch (Exception e) {
            throw new RuntimeException("Cloudinary dosya yükleme hatası: " + e.getMessage());
        }
    }

    private String saveToLocal(MultipartFile file) {
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
                String candidate = originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
                candidate = candidate.replaceAll("[^a-z0-9]", "");
                if (!candidate.isBlank() && candidate.length() <= 8) {
                    extension = "." + candidate;
                }
            }
        }
        return UUID.randomUUID().toString().replace("-", "") + extension;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String toStringOrNull(Object value) {
        if (value == null) return null;
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
