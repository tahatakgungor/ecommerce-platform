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
        log.info("User schema patch tamamlandı (password_reset_token_expires_at).");
    }

    private void executeSql(String sql) {
        jdbcTemplate.execute(sql);
    }
}
