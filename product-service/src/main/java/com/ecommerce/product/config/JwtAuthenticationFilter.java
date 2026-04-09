package com.ecommerce.product.config;

import com.ecommerce.product.application.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Authorization header ve cookie token'larını ayrı ayrı al.
        String headerToken = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            headerToken = authHeader.substring(7);
        }

        String cookieToken = null;
        if (request.getCookies() != null) {
            cookieToken = Arrays.stream(request.getCookies())
                    .filter(c -> "access_token".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        String token = headerToken != null ? headerToken : cookieToken;
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean authenticated = tryAuthenticate(token);
        if (!authenticated && headerToken != null && cookieToken != null && !headerToken.equals(cookieToken)) {
            // Header token süresi dolmuş/bozuk olabilir; cookie token ile tekrar dene.
            tryAuthenticate(cookieToken);
        }

        filterChain.doFilter(request, response);
    }

    private boolean tryAuthenticate(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));

            if (email != null && role != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(role))
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
            return true;
        } catch (Exception e) {
            log.debug("JWT doğrulama başarısız: {}", e.getMessage());
            return false;
        }
    }
}
