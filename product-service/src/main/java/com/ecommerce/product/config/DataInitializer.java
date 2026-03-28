package com.ecommerce.product.config;

import com.ecommerce.product.domain.User;
import com.ecommerce.product.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        // Eğer sistemde hiç kullanıcı yoksa bir default admin oluştur
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setName("Default Admin");
            admin.setEmail("admin@taha.com");
            admin.setPassword("123456"); // Şimdilik plain text (AuthService'in beklediği gibi)
            admin.setRole("ADMIN");

            userRepository.save(admin);
            System.out.println(">> Default Admin oluşturuldu: admin@taha.com / 123456");
        }
    }
}