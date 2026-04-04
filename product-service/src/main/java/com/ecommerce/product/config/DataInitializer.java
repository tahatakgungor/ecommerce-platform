package com.ecommerce.product.config;

import com.ecommerce.product.domain.User;
import com.ecommerce.product.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // Eklendi
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder; // SecurityConfig'deki bean'i alıyoruz

    @Value("${app.seed-default-admin:false}")
    private boolean seedDefaultAdmin;

    @Value("${app.default-admin.email:admin@taha.com}")
    private String defaultAdminEmail;

    @Value("${app.default-admin.password:}")
    private String defaultAdminPassword;

    @Override
    public void run(String... args) throws Exception {
        if (!seedDefaultAdmin) {
            return;
        }

        // Eğer sistemde hiç kullanıcı yoksa bir default admin oluştur
        if (userRepository.count() == 0) {
            if (defaultAdminPassword == null || defaultAdminPassword.isBlank()) {
                throw new IllegalStateException("Default admin seed aktif ama app.default-admin.password boş!");
            }

            User admin = new User();
            admin.setName("Default Admin");
            admin.setEmail(defaultAdminEmail.trim().toLowerCase());

            // KRİTİK NOKTA: Şifreyi kaydederken hash'liyoruz
            String encodedPassword = passwordEncoder.encode(defaultAdminPassword);
            admin.setPassword(encodedPassword);

            admin.setRole("Admin");

            userRepository.save(admin);
            log.warn("Default admin hesabı oluşturuldu: {}", admin.getEmail());
        }
    }
}
