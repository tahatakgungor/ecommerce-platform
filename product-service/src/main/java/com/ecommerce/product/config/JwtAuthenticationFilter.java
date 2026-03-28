package com.ecommerce.product.config;

import com.ecommerce.product.application.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String email = jwtService.extractEmail(token);
            // KRİTİK: Rolü token'dan dinamik olarak çekiyoruz
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));

            if (email != null && role != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Controller'da hasAuthority('Admin') dediğimiz için buraya direkt role ismini veriyoruz.
                // Eğer hasRole('Admin') deseydik "ROLE_" + role yapmamız gerekirdi.
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(role))
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // Token geçersizse veya parse edilemiyorsa sessizce devam et, Security kapıyı kapatacaktır.
            System.err.println("JWT doğrulama hatası: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}