package com.ecommerce.product.application;

import com.ecommerce.product.dto.LoginRequest;
import com.ecommerce.product.dto.LoginResponse;
import com.ecommerce.product.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    public LoginResponse login(LoginRequest request) {
        // ŞİMDİLİK: Basit bir kontrol yapıyoruz.
        // Gerçek projede burada şifre hash kontrolü ve JWT üretimi olur.
        if ("admin@test.com".equals(request.email()) && "123456".equals(request.password())) {
            return new LoginResponse(
                    "fake-jwt-token-harr-123", // Frontend'i içeri sokacak anahtar
                    request.email(),
                    "Admin User",
                    "ADMIN"
            );
        }
        throw new RuntimeException("E-posta veya şifre hatalı!");
    }
}