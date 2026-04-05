package com.ecommerce.product.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class UserSchemaPatchRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        Boolean usersTableExists = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                      AND table_name = 'users'
                )
                """,
                Boolean.class
        );

        if (!Boolean.TRUE.equals(usersTableExists)) {
            log.warn("User schema patch atlandı: public.users tablosu bulunamadı.");
            return;
        }

        executeSql("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_token_expires_at TIMESTAMP");
        executeSql("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_change_verification_code_hash VARCHAR(255)");
        executeSql("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_change_verification_expires_at TIMESTAMP");
        executeSql("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_change_pending_password_hash VARCHAR(255)");
        executeSql("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_change_verification_attempts INTEGER DEFAULT 0");
        executeSql("UPDATE users SET password_change_verification_attempts = 0 WHERE password_change_verification_attempts IS NULL");
        log.info("User schema patch tamamlandı (reset + change-password verification kolonları).");
    }

    private void executeSql(String sql) {
        jdbcTemplate.execute(sql);
    }
}
