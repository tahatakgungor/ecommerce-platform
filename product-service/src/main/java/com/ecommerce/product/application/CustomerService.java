package com.ecommerce.product.application;

import com.ecommerce.product.domain.User;
import com.ecommerce.product.dto.auth.CustomerLoginResponse;
import com.ecommerce.product.dto.auth.CustomerSignupRequest;
import com.ecommerce.product.dto.auth.CustomerUserDto;
import com.ecommerce.product.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Transactional
    public CustomerLoginResponse signup(CustomerSignupRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new RuntimeException("Email boş olamaz!");
        }
        if (request.password() == null || request.password().length() < 6) {
            throw new RuntimeException("Şifre en az 6 karakter olmalıdır!");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new RuntimeException("Şifreler eşleşmiyor!");
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Bu email adresi zaten kullanımda!");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("Customer");
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new CustomerLoginResponse(token, toDto(user));
    }

    public CustomerLoginResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("E-posta veya şifre hatalı!"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("E-posta veya şifre hatalı!");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new CustomerLoginResponse(token, toDto(user));
    }

    public CustomerUserDto getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));
        return toDto(user);
    }

    @Transactional
    public void forgetPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Bu email adresine kayıtlı hesap bulunamadı!"));

        String resetToken = UUID.randomUUID().toString().replace("-", "");
        user.setPasswordResetToken(resetToken);
        userRepository.save(user);

        try {
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        } catch (Exception ignored) {
            // Mail gönderilemese bile token kaydedildi; production'da loglanmalı
        }
    }

    @Transactional
    public void confirmForgetPassword(String token, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Şifreler eşleşmiyor!");
        }

        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new RuntimeException("Geçersiz veya süresi dolmuş sıfırlama bağlantısı!"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Mevcut şifre hatalı!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public CustomerLoginResponse updateUser(UUID id, Map<String, String> updates) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        if (updates.containsKey("name") && updates.get("name") != null) {
            user.setName(updates.get("name"));
        }
        if (updates.containsKey("phone")) {
            user.setPhone(updates.get("phone"));
        }
        if (updates.containsKey("email") && updates.get("email") != null) {
            String newEmail = updates.get("email");
            if (!newEmail.equals(user.getEmail())) {
                if (userRepository.findByEmail(newEmail).isPresent()) {
                    throw new RuntimeException("Bu email adresi zaten kullanımda!");
                }
                user.setEmail(newEmail);
            }
        }

        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new CustomerLoginResponse(token, toDto(user));
    }

    private CustomerUserDto toDto(User user) {
        return new CustomerUserDto(
                user.getId().toString(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPhone()
        );
    }
}
