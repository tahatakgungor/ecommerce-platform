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
@Order(0)
public class CouponSchemaPatchRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        Boolean couponsTableExists = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                      AND table_name = 'coupons'
                )
                """,
                Boolean.class
        );

        if (!Boolean.TRUE.equals(couponsTableExists)) {
            log.warn("Coupon schema patch atlandı: public.coupons tablosu bulunamadı.");
            return;
        }

        executeSql("ALTER TABLE coupons ADD COLUMN IF NOT EXISTS scope VARCHAR(32)");
        executeSql("ALTER TABLE coupons ADD COLUMN IF NOT EXISTS assigned_user_email VARCHAR(255)");
        executeSql("ALTER TABLE coupons ADD COLUMN IF NOT EXISTS assigned_user_id VARCHAR(255)");
        executeSql("ALTER TABLE coupons ADD COLUMN IF NOT EXISTS product_scope VARCHAR(32)");
        executeSql("UPDATE coupons SET scope = 'PUBLIC' WHERE scope IS NULL OR BTRIM(scope) = ''");
        executeSql("UPDATE coupons SET product_scope = 'CATEGORY' WHERE product_scope IS NULL OR BTRIM(product_scope) = ''");
        executeSql("ALTER TABLE coupons ALTER COLUMN scope SET DEFAULT 'PUBLIC'");
        executeSql("ALTER TABLE coupons ALTER COLUMN scope SET NOT NULL");
        executeSql("ALTER TABLE coupons ALTER COLUMN product_scope SET DEFAULT 'CATEGORY'");
        executeSql("ALTER TABLE coupons ALTER COLUMN product_scope SET NOT NULL");
        executeSql("ALTER TABLE coupons ALTER COLUMN product_type DROP NOT NULL");

        log.info("Coupon schema patch tamamlandı (scope, product_scope, assignment alanları).");
    }

    private void executeSql(String sql) {
        jdbcTemplate.execute(sql);
    }
}
