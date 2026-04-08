package com.ecommerce.product.application;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityRateLimitServiceTest {

    @Test
    void assertAllowed_shouldThrowWhenAttemptsReachLimit() {
        SecurityRateLimitService service = new SecurityRateLimitService();
        String key = "rate-limit-test";
        Duration window = Duration.ofMinutes(5);

        service.registerFailure(key, window);
        service.registerFailure(key, window);

        assertThrows(ResponseStatusException.class, () ->
                service.assertAllowed(key, 2, window, "RATE_LIMIT_EXCEEDED:TEST"));
    }

    @Test
    void clearFailures_shouldResetLimitWindow() {
        SecurityRateLimitService service = new SecurityRateLimitService();
        String key = "rate-limit-clear-test";
        Duration window = Duration.ofMinutes(5);

        service.registerFailure(key, window);
        service.registerFailure(key, window);
        service.clearFailures(key);

        service.assertAllowed(key, 2, window, "RATE_LIMIT_EXCEEDED:TEST");
    }
}
