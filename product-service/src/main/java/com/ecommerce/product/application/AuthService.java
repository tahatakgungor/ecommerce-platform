package com.ecommerce.product.application;

import com.ecommerce.product.domain.User;
import com.ecommerce.product.dto.auth.LoginRequest;
import com.ecommerce.product.dto.auth.LoginResponse;
import com.ecommerce.product.dto.auth.RegisterRequest;
import com.ecommerce.product.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    // Şimdilik BCrypt eklemediysen direkt String kontrolü yaparız,
    // ama gerçek projede passwordEncoder kullanmalısın.

    public LoginResponse login(LoginRequest request) {
        // 1. Kullanıcıyı veritabanında ara
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        // 2. Şifre kontrolü (Şimdilik düz metin, ileride encode edilecek)
        if (!user.getPassword().equals(request.password())) {
            throw new RuntimeException("E-posta veya şifre hatalı!");
        }

        // 3. Başarılı giriş: Kullanıcı bilgilerini dön
        // NOT: "fake-jwt-token..." kısmını ileride gerçek JWT ile değiştireceğiz.
        return new LoginResponse(
                "fake-jwt-token-" + user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }

    @Transactional
    public void registerWithRole(RegisterRequest request, String role) {
        // Email mükerrer kayıt kontrolü
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Bu email zaten kullanımda!");
        }

        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(request.getPassword()); // İleride: passwordEncoder.encode(...)
        newUser.setName(request.getName());
        newUser.setRole(role); // Davetiyeden gelen rol (ADMIN/STAFF)

        userRepository.save(newUser);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // YENİ: ID ile kullanıcı bul
    public User getUserById(java.util.UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));
    }

    // YENİ: Kullanıcı sil
    @Transactional
    public void deleteUser(java.util.UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Silinmek istenen kullanıcı bulunamadı.");
        }
        userRepository.deleteById(id);
    }
}