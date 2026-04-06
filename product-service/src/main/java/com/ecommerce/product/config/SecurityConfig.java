package com.ecommerce.product.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Value("${app.cors.allowed-origin-patterns:http://localhost:3000,http://localhost:3001}")
    private String allowedOriginPatternsRaw;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.admin-frontend-url:http://localhost:3001}")
    private String adminFrontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieName("XSRF-TOKEN");
        csrfTokenRepository.setHeaderName("X-XSRF-TOKEN");
        csrfTokenRepository.setCookiePath("/");
        RequestMatcher bearerTokenRequestMatcher = this::hasBearerAuthorization;

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'"
                        ))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .permissionsPolicy(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=(), payment=()"))
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .ignoringRequestMatchers(bearerTokenRequestMatcher)
                        .ignoringRequestMatchers(
                                "/api/admin/login",
                                "/api/admin/register",
                                "/api/admin/forget-password",
                                "/api/admin/confirm-forget-password",
                                "/api/user/signup",
                                "/api/user/login",
                                "/api/user/confirmEmail/**",
                                "/api/user/forget-password",
                                "/api/user/confirm-forget-password",
                                "/api/contact/send",
                                "/api/user/newsletter"
                        ))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Admin auth endpoint'leri
                        .requestMatchers("/api/admin/login").permitAll()
                        .requestMatchers("/api/admin/register").permitAll()
                        .requestMatchers("/api/admin/forget-password").permitAll()
                        .requestMatchers("/api/admin/confirm-forget-password").permitAll()
                        // Müşteri auth endpoint'leri
                        .requestMatchers("/api/user/signup").permitAll()
                        .requestMatchers("/api/user/login").permitAll()
                        .requestMatchers("/api/user/confirmEmail/**").permitAll()
                        .requestMatchers("/api/user/forget-password").permitAll()
                        .requestMatchers("/api/user/confirm-forget-password").permitAll()
                        // Müşteri tarafı public endpoint'ler
                        .requestMatchers("/api/products/show").permitAll()
                        .requestMatchers("/api/products/{id}").permitAll()
                        .requestMatchers("/api/products/discount").permitAll()
                        .requestMatchers("/api/products/relatedProduct").permitAll()
                        .requestMatchers("/api/banners/show").permitAll()
                        .requestMatchers("/api/blog").permitAll()
                        .requestMatchers("/api/blog/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/*/reviews").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/*/reviews/summary").permitAll()
                        .requestMatchers("/api/category/show").permitAll()
                        .requestMatchers("/api/brand/all").permitAll()
                        .requestMatchers("/api/category/all").permitAll()
                        .requestMatchers("/api/coupon").permitAll()
                        // İletişim formu - public
                        .requestMatchers("/api/contact/send").permitAll()
                        // Newsletter - giriş yapmadan abone olunabilir
                        .requestMatchers("/api/user/newsletter").permitAll()
                        // Logout - herkes erişebilir (cookie temizleme)
                        .requestMatchers("/api/user/logout").permitAll()
                        // Static dosyalar
                        .requestMatchers("/uploads/**").permitAll()
                        // Geri kalan her şey authentication gerektirir
                        .anyRequest().authenticated()
                );

        // SPA istemcilerinin XSRF-TOKEN cookie'sini her zaman alabilmesi için token'ı eager üret.
        http.addFilterAfter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                if (csrfToken != null) {
                    csrfToken.getToken();
                }
                filterChain.doFilter(request, response);
            }
        }, CsrfFilter.class);

        http.addFilterBefore(jwtAuthFilter,
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    boolean hasBearerAuthorization(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        return authHeader != null
                && authHeader.startsWith("Bearer ")
                && authHeader.length() > "Bearer ".length();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> allowedOriginPatterns = buildAllowedOriginPatterns();

        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "X-XSRF-TOKEN"
        ));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    List<String> buildAllowedOriginPatterns() {
        LinkedHashSet<String> patterns = new LinkedHashSet<>();

        patterns.addAll(Arrays.stream(allowedOriginPatternsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList()));

        addOriginIfPresent(patterns, frontendUrl);
        addOriginIfPresent(patterns, adminFrontendUrl);
        return new ArrayList<>(patterns);
    }

    private void addOriginIfPresent(LinkedHashSet<String> patterns, String url) {
        if (url == null || url.isBlank()) return;
        try {
            URI uri = URI.create(url.trim());
            if (uri.getScheme() == null || uri.getHost() == null) return;
            String origin = uri.getScheme().toLowerCase() + "://" + uri.getHost().toLowerCase();
            if (uri.getPort() != -1) {
                origin += ":" + uri.getPort();
            }
            patterns.add(origin);
        } catch (Exception ignored) {
            // invalid url değeri CORS listesine eklenmez
        }
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
