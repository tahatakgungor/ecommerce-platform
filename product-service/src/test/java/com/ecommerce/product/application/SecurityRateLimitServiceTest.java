package com.ecommerce.product.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityRateLimitServiceTest {

    @Test
    void shouldBlockWhenAttemptsReachLimit() {
        SecurityRateLimitService service = new SecurityRateLimitService();
        Duration window = Duration.ofMinutes(15);
        String key = "login:test@example.com:127.0.0.1";

        service.registerFailure(key, window);
        service.registerFailure(key, window);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.assertAllowed(key, 2, window, "blocked")
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    }

    @Test
    void shouldAllowAgainAfterClearFailures() {
        SecurityRateLimitService service = new SecurityRateLimitService();
        Duration window = Duration.ofMinutes(15);
        String key = "login:test@example.com:127.0.0.1";

        service.registerFailure(key, window);
        service.registerFailure(key, window);
        service.clearFailures(key);

        assertDoesNotThrow(() -> service.assertAllowed(key, 2, window, "blocked"));
    }

    @Test
    void shouldPruneOldAttemptsOutsideWindow() throws InterruptedException {
        SecurityRateLimitService service = new SecurityRateLimitService();
        Duration window = Duration.ofMillis(20);
        String key = "login:test@example.com:127.0.0.1";

        service.registerFailure(key, window);
        Thread.sleep(35);

        assertDoesNotThrow(() -> service.assertAllowed(key, 1, window, "blocked"));
    }
}
