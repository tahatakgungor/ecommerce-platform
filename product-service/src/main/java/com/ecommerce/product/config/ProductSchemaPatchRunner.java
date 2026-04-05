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
@Order(2)
public class ProductSchemaPatchRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        Boolean productsTableExists = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                      AND table_name = 'products'
                )
                """,
                Boolean.class
        );

        if (!Boolean.TRUE.equals(productsTableExists)) {
            log.warn("Product schema patch atlandı: public.products tablosu bulunamadı.");
            return;
        }

        jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS created_at TIMESTAMP");
        jdbcTemplate.execute("UPDATE products SET created_at = NOW() WHERE created_at IS NULL");
        log.info("Product schema patch tamamlandı (created_at).");
    }
}
