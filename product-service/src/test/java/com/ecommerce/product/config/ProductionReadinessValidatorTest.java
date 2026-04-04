package com.ecommerce.product.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionReadinessValidatorTest {

    @Test
    void run_shouldSkipWhenValidationDisabledAndNoProdProfile() {
        MockEnvironment env = new MockEnvironment();
        ProductionReadinessValidator validator = new ProductionReadinessValidator(env);

        ReflectionTestUtils.setField(validator, "requireStartupValidation", false);
        ReflectionTestUtils.setField(validator, "jwtSecret", "");
        ReflectionTestUtils.setField(validator, "frontendUrl", "");
        ReflectionTestUtils.setField(validator, "adminFrontendUrl", "");
        ReflectionTestUtils.setField(validator, "corsAllowedOriginPatterns", "");
        ReflectionTestUtils.setField(validator, "resendApiKey", "");
        ReflectionTestUtils.setField(validator, "resendFromEmail", "");

        assertDoesNotThrow(() -> validator.run());
    }

    @Test
    void run_shouldFailWhenProdProfileAndMissingValues() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");
        ProductionReadinessValidator validator = new ProductionReadinessValidator(env);

        ReflectionTestUtils.setField(validator, "requireStartupValidation", false);
        ReflectionTestUtils.setField(validator, "jwtSecret", "local-dev-secret-key-must-be-at-least-32-chars");
        ReflectionTestUtils.setField(validator, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(validator, "adminFrontendUrl", "http://localhost:3001");
        ReflectionTestUtils.setField(validator, "corsAllowedOriginPatterns", "");
        ReflectionTestUtils.setField(validator, "resendApiKey", "");
        ReflectionTestUtils.setField(validator, "resendFromEmail", "");

        assertThrows(IllegalStateException.class, validator::run);
    }

    @Test
    void run_shouldPassWhenProdProfileAndAllValuesPresent() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        ProductionReadinessValidator validator = new ProductionReadinessValidator(env);

        ReflectionTestUtils.setField(validator, "requireStartupValidation", false);
        ReflectionTestUtils.setField(validator, "jwtSecret", "a-very-strong-production-jwt-secret-value");
        ReflectionTestUtils.setField(validator, "frontendUrl", "https://storefront.example.com");
        ReflectionTestUtils.setField(validator, "adminFrontendUrl", "https://admin.example.com");
        ReflectionTestUtils.setField(validator, "corsAllowedOriginPatterns", "https://storefront.example.com,https://admin.example.com");
        ReflectionTestUtils.setField(validator, "resendApiKey", "re_test_123");
        ReflectionTestUtils.setField(validator, "resendFromEmail", "no-reply@example.com");

        assertDoesNotThrow(() -> validator.run());
    }

    @Test
    void run_shouldFailWhenProdProfileContainsLocalhostCors() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        ProductionReadinessValidator validator = new ProductionReadinessValidator(env);

        ReflectionTestUtils.setField(validator, "requireStartupValidation", false);
        ReflectionTestUtils.setField(validator, "jwtSecret", "a-very-strong-production-jwt-secret-value");
        ReflectionTestUtils.setField(validator, "frontendUrl", "https://storefront.example.com");
        ReflectionTestUtils.setField(validator, "adminFrontendUrl", "https://admin.example.com");
        ReflectionTestUtils.setField(validator, "corsAllowedOriginPatterns", "http://localhost:3000,https://storefront.example.com");
        ReflectionTestUtils.setField(validator, "resendApiKey", "re_test_123");
        ReflectionTestUtils.setField(validator, "resendFromEmail", "no-reply@example.com");

        assertThrows(IllegalStateException.class, validator::run);
    }

    @Test
    void run_shouldPassWhenProdProfileAndCorsPatternsBlankButFrontendUrlsPresent() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");
        ProductionReadinessValidator validator = new ProductionReadinessValidator(env);

        ReflectionTestUtils.setField(validator, "requireStartupValidation", false);
        ReflectionTestUtils.setField(validator, "jwtSecret", "a-very-strong-production-jwt-secret-value");
        ReflectionTestUtils.setField(validator, "frontendUrl", "https://ecommerce-frontend-kappa-six.vercel.app");
        ReflectionTestUtils.setField(validator, "adminFrontendUrl", "https://ecommerce-admin.vercel.app");
        ReflectionTestUtils.setField(validator, "corsAllowedOriginPatterns", "");
        ReflectionTestUtils.setField(validator, "resendApiKey", "re_test_123");
        ReflectionTestUtils.setField(validator, "resendFromEmail", "no-reply@example.com");

        assertDoesNotThrow(() -> validator.run());
    }
}
