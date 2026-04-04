package com.ecommerce.product.application;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SecurityRateLimitService {

    private final Map<String, ArrayDeque<Instant>> failures = new ConcurrentHashMap<>();

    public void assertAllowed(String key, int maxAttempts, Duration window, String message) {
        ArrayDeque<Instant> attempts = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts, window);
            if (attempts.size() >= maxAttempts) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, message);
            }
        }
    }

    public void registerFailure(String key, Duration window) {
        ArrayDeque<Instant> attempts = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts, window);
            attempts.addLast(Instant.now());
        }
    }

    public void clearFailures(String key) {
        failures.remove(key);
    }

    private void prune(ArrayDeque<Instant> attempts, Duration window) {
        Instant threshold = Instant.now().minus(window);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(threshold)) {
            attempts.pollFirst();
        }
    }
}
