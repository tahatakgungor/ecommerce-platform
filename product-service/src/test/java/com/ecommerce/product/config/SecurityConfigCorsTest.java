package com.ecommerce.product.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigCorsTest {

    @Test
    void buildAllowedOriginPatterns_shouldIncludeFrontendAndAdminOrigins() {
        SecurityConfig config = new SecurityConfig(null);
        ReflectionTestUtils.setField(config, "allowedOriginPatternsRaw", "http://localhost:3000");
        ReflectionTestUtils.setField(config, "frontendUrl", "https://ecommerce-frontend-kappa-six.vercel.app/some-path");
        ReflectionTestUtils.setField(config, "adminFrontendUrl", "https://ecommerce-admin.vercel.app");

        List<String> patterns = config.buildAllowedOriginPatterns();

        assertTrue(patterns.contains("http://localhost:3000"));
        assertTrue(patterns.contains("https://ecommerce-frontend-kappa-six.vercel.app"));
        assertTrue(patterns.contains("https://ecommerce-admin.vercel.app"));
    }

    @Test
    void corsConfigurationSource_shouldAllowFrontendOriginFromFrontendUrl() {
        SecurityConfig config = new SecurityConfig(null);
        ReflectionTestUtils.setField(config, "allowedOriginPatternsRaw", "http://localhost:3000");
        ReflectionTestUtils.setField(config, "frontendUrl", "https://ecommerce-frontend-kappa-six.vercel.app");
        ReflectionTestUtils.setField(config, "adminFrontendUrl", "https://ecommerce-admin.vercel.app");

        CorsConfigurationSource source = config.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products/show");
        request.addHeader("Origin", "https://ecommerce-frontend-kappa-six.vercel.app");

        CorsConfiguration cors = ((UrlBasedCorsConfigurationSource) source).getCorsConfiguration(request);
        assertNotNull(cors);
        assertEquals("https://ecommerce-frontend-kappa-six.vercel.app",
                cors.checkOrigin("https://ecommerce-frontend-kappa-six.vercel.app"));
    }

    @Test
    void corsConfigurationSource_shouldExposeExpectedCorsPolicy() {
        SecurityConfig config = new SecurityConfig(null);
        ReflectionTestUtils.setField(config, "allowedOriginPatternsRaw", "");
        ReflectionTestUtils.setField(config, "frontendUrl", "https://ecommerce-frontend-kappa-six.vercel.app");
        ReflectionTestUtils.setField(config, "adminFrontendUrl", "https://ecommerce-admin.vercel.app");

        CorsConfigurationSource source = config.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/products/show");
        request.addHeader("Origin", "https://ecommerce-frontend-kappa-six.vercel.app");
        request.addHeader("Access-Control-Request-Method", "GET");

        CorsConfiguration cors = ((UrlBasedCorsConfigurationSource) source).getCorsConfiguration(request);
        assertNotNull(cors);
        assertTrue(Boolean.TRUE.equals(cors.getAllowCredentials()));
        assertTrue(cors.getAllowedMethods().contains("OPTIONS"));
        assertTrue(cors.getAllowedMethods().contains("GET"));
        assertEquals("https://ecommerce-frontend-kappa-six.vercel.app",
                cors.checkOrigin("https://ecommerce-frontend-kappa-six.vercel.app"));
    }
}
