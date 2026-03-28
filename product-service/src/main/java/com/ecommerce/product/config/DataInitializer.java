package com.ecommerce.product.config;

import com.ecommerce.product.domain.User;
import com.ecommerce.product.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // Eklendi
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder; // SecurityConfig'deki bean'i alıyoruz

    @Override
    public void run(String... args) throws Exception {
        // Eğer sistemde hiç kullanıcı yoksa bir default admin oluştur
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setName("Default Admin");
            admin.setEmail("admin@taha.com");

            // KRİTİK NOKTA: Şifreyi kaydederken hash'liyoruz
            String encodedPassword = passwordEncoder.encode("123456");
            admin.setPassword(encodedPassword);

            admin.setRole("Admin");

            userRepository.save(admin);
            System.out.println(">> Default Admin BCrypt ile oluşturuldu: admin@taha.com / 123456");
        }
    }
}