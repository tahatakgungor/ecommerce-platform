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
public class SiteAndReturnSchemaPatchRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        executeSql("""
                CREATE TABLE IF NOT EXISTS site_settings (
                    id UUID PRIMARY KEY,
                    singleton_key VARCHAR(40) NOT NULL UNIQUE,
                    announcement_text_tr VARCHAR(255),
                    announcement_text_en VARCHAR(255),
                    announcement_link VARCHAR(500),
                    announcement_active BOOLEAN NOT NULL DEFAULT FALSE,
                    announcement_speed INTEGER NOT NULL DEFAULT 40,
                    whatsapp_number VARCHAR(40),
                    whatsapp_label VARCHAR(120),
                    support_email VARCHAR(320),
                    support_phone VARCHAR(40),
                    return_window_days INTEGER NOT NULL DEFAULT 14,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP
                )
                """);

        executeSql("""
                CREATE TABLE IF NOT EXISTS order_returns (
                    id UUID PRIMARY KEY,
                    order_id UUID NOT NULL,
                    user_id VARCHAR(255),
                    user_email VARCHAR(255) NOT NULL,
                    guest_order BOOLEAN NOT NULL DEFAULT FALSE,
                    reason VARCHAR(500) NOT NULL,
                    customer_note TEXT,
                    status VARCHAR(20) NOT NULL,
                    status_history TEXT,
                    admin_note TEXT,
                    processed_by VARCHAR(255),
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP
                )
                """);
        executeSql("CREATE INDEX IF NOT EXISTS idx_order_returns_order_id ON order_returns(order_id)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_order_returns_user_email ON order_returns(user_email)");

        log.info("SiteSettings ve OrderReturn schema patch tamamlandı.");
    }

    private void executeSql(String sql) {
        jdbcTemplate.execute(sql);
    }
}
