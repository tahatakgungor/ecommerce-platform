package com.ecommerce.product.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigCsrfBypassTest {

    @Test
    void hasBearerAuthorization_shouldReturnTrueForValidBearerHeader() {
        SecurityConfig config = new SecurityConfig(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-value");

        assertTrue(config.hasBearerAuthorization(request));
    }

    @Test
    void hasBearerAuthorization_shouldReturnFalseWhenHeaderMissingOrInvalid() {
        SecurityConfig config = new SecurityConfig(null);

        MockHttpServletRequest noHeader = new MockHttpServletRequest();
        assertFalse(config.hasBearerAuthorization(noHeader));

        MockHttpServletRequest wrongScheme = new MockHttpServletRequest();
        wrongScheme.addHeader("Authorization", "Basic abc");
        assertFalse(config.hasBearerAuthorization(wrongScheme));

        MockHttpServletRequest emptyBearer = new MockHttpServletRequest();
        emptyBearer.addHeader("Authorization", "Bearer ");
        assertFalse(config.hasBearerAuthorization(emptyBearer));
    }
}

