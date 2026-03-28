package com.ecommerce.product.application;

import com.ecommerce.product.domain.User;
import com.ecommerce.product.dto.auth.LoginRequest;
import com.ecommerce.product.dto.auth.LoginResponse;
import com.ecommerce.product.dto.auth.RegisterRequest;
import com.ecommerce.product.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService; // Yeni enjekte edilen JWT Servisi

    /**
     * Kullanıcı girişi ve Gerçek JWT üretimi
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        // BCrypt şifre doğrulaması
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("E-posta veya şifre hatalı!");
        }

        // GERÇEK JWT TOKEN ÜRETİMİ
        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        return new LoginResponse(
                token,
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getId()
        );
    }

    /**
     * Yeni kullanıcı kaydı (Şifre hashlenerek kaydedilir)
     */
    @Transactional
    public void registerWithRole(RegisterRequest request, String role) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Bu email zaten kullanımda!");
        }

        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword())); // Şifre zırhlandı
        newUser.setName(request.getName());
        newUser.setRole(role);

        userRepository.save(newUser);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));
    }

    /**
     * Personel Silme - Sadece ADMIN yetkisiyle
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Silinmek istenen kullanıcı bulunamadı.");
        }
        userRepository.deleteById(id);
    }

    /**
     * Profil/Personel Güncelleme
     * Güvenlik: Ya ADMIN olmalı ya da kullanıcı kendi profilini güncellemeli
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.principal.id")
    public LoginResponse updateUser(UUID id, RegisterRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Güncellenecek kullanıcı bulunamadı!"));

        // Temel bilgileri güncelle
        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // Şifre alanı doluysa güncelle, boşsa mevcut şifreyi koru
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);

        // Bilgiler değiştiği için yeni bir Token üretiyoruz (opsiyonel ama sağlıklı)
        String newToken = jwtService.generateToken(user.getEmail(), user.getRole());

        return new LoginResponse(
                newToken,
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getId()
        );
    }
}