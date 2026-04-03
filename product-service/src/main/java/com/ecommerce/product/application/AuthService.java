package com.ecommerce.product.application;

import com.ecommerce.product.domain.User;
import com.ecommerce.product.dto.auth.LoginRequest;
import com.ecommerce.product.dto.auth.LoginResponse;
import com.ecommerce.product.dto.auth.RegisterRequest;
import com.ecommerce.product.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final JwtService jwtService;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:3001}")
    private String adminFrontendUrl;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("E-posta veya şifre hatalı!");
        }

        // Müşteri hesabıyla admin panele giriş engellenir
        if ("Customer".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Bu hesap admin panele erişim yetkisine sahip değil.");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        return new LoginResponse(
                token,
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getId()
        );
    }

    @Transactional
    public void registerWithRole(RegisterRequest request, String role) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Bu email zaten kullanımda!");
        }

        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setName(request.getName());
        newUser.setRole(role != null ? role : "Staff");

        userRepository.save(newUser);
    }

    @Transactional
    public void forgetPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Bu e-posta adresine kayıtlı admin hesabı bulunamadı!"));

        if ("Customer".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Bu hesap admin paneline ait değil.");
        }

        String resetToken = UUID.randomUUID().toString().replace("-", "");
        user.setPasswordResetToken(resetToken);
        userRepository.save(user);

        try {
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        } catch (Exception e) {
            // Mail gönderilemese bile token kaydedildi
        }
    }

    @Transactional
    public void confirmForgetPassword(String token, String password) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new RuntimeException("Geçersiz veya süresi dolmuş sıfırlama bağlantısı!"));

        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordResetToken(null);
        userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findByRoleIn(List.of("Admin", "Staff"));
    }

    public List<User> getAllCustomers() {
        return userRepository.findByRole("Customer");
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));
    }

    /**
     * PERSONEL SİLME - %100 BACKEND GÜVENLİĞİ
     */
    @Transactional
    @PreAuthorize("hasAuthority('Admin')")
    public void deleteUser(UUID id) {
        // 1. Silinmek istenen kullanıcı var mı?
        User userToDelete = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        // 2. BACKEND ÇEKİ: İsteği atan kişinin e-postasını direkt SecurityContext'ten (JWT'den gelen veri) alıyoruz.
        // Frontend ne gönderirse göndersin, biz sistemdeki aktif kullanıcıya bakıyoruz.
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // 3. Kendi kendini silme denemesi mi?
        if (userToDelete.getEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new RuntimeException("Güvenlik ihlali: Kendi hesabınızı silemezsiniz!");
        }

        userRepository.delete(userToDelete);
    }

    /**
     * ROL GÜNCELLEME - Sadece Admin yapabilir
     */
    @Transactional
    @PreAuthorize("hasAuthority('Admin')")
    public void updateUserRole(UUID id, String newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        // Backend'de rolü sanitize ediyoruz (Sadece Admin veya Staff olabilir)
        if (newRole.equalsIgnoreCase("Admin")) user.setRole("Admin");
        else user.setRole("Staff");

        userRepository.save(user);
    }

    /**
     * PROFİL GÜNCELLEME
     * Ya ADMIN olmalı ya da ID, login olan kullanıcının kendi ID'si olmalı.
     */
    @Transactional
    @PreAuthorize("hasAuthority('Admin') or #id.toString() == authentication.principal.id")
    public LoginResponse updateUser(UUID id, RegisterRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);

        // Bilgiler değiştiği için yetkileri de içeren taze bir token dönüyoruz.
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