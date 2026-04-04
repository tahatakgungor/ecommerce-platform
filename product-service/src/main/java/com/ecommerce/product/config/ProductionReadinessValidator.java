package com.ecommerce.product.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(2)
public class ProductionReadinessValidator implements CommandLineRunner {

    private final Environment environment;

    @Value("${app.security.require-startup-validation:false}")
    private boolean requireStartupValidation;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.frontend-url:}")
    private String frontendUrl;

    @Value("${app.admin-frontend-url:}")
    private String adminFrontendUrl;

    @Value("${app.cors.allowed-origin-patterns:}")
    private String corsAllowedOriginPatterns;

    @Value("${app.mail.resend.api-key:}")
    private String resendApiKey;

    @Value("${app.mail.resend.from-email:}")
    private String resendFromEmail;

    @Override
    public void run(String... args) {
        if (!shouldValidate()) return;

        List<String> missing = new ArrayList<>();

        if (isBlank(jwtSecret) || jwtSecret.contains("local-dev-secret-key")) {
            missing.add("app.jwt.secret (JWT_SECRET)");
        }
        if (isBlank(frontendUrl)) {
            missing.add("app.frontend-url (FRONTEND_URL)");
        }
        if (isBlank(adminFrontendUrl)) {
            missing.add("app.admin-frontend-url (ADMIN_FRONTEND_URL)");
        }
        if (isBlank(corsAllowedOriginPatterns) && isBlank(frontendUrl) && isBlank(adminFrontendUrl)) {
            missing.add("app.cors.allowed-origin-patterns (CORS_ALLOWED_ORIGIN_PATTERNS)");
        }
        if (isBlank(resendApiKey)) {
            missing.add("app.mail.resend.api-key (RESEND_API_KEY)");
        }
        if (isBlank(resendFromEmail)) {
            missing.add("app.mail.resend.from-email (RESEND_FROM_EMAIL)");
        }

        if (isProductionProfileActive()) {
            if (!isHttpsUrl(frontendUrl)) {
                missing.add("app.frontend-url production'da https olmalı");
            }
            if (!isHttpsUrl(adminFrontendUrl)) {
                missing.add("app.admin-frontend-url production'da https olmalı");
            }
            if (!isBlank(corsAllowedOriginPatterns) && corsAllowedOriginPatterns.toLowerCase().contains("localhost")) {
                missing.add("app.cors.allowed-origin-patterns production'da localhost içermemeli");
            }
            if (!resendFromEmail.contains("@")) {
                missing.add("app.mail.resend.from-email geçerli bir e-posta olmalı");
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException("Production startup validation başarısız. Eksik/Hatalı ayarlar: " + String.join(", ", missing));
        }
    }

    private boolean shouldValidate() {
        return requireStartupValidation || isProductionProfileActive();
    }

    private boolean isProductionProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));
    }

    private boolean isHttpsUrl(String value) {
        return !isBlank(value) && value.trim().toLowerCase().startsWith("https://");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
