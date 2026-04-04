package com.ecommerce.product.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponSchemaPatchRunnerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void run_shouldSkipWhenCouponsTableMissing() {
        when(jdbcTemplate.queryForObject(contains("table_name = 'coupons'"), eq(Boolean.class)))
                .thenReturn(false);

        CouponSchemaPatchRunner runner = new CouponSchemaPatchRunner(jdbcTemplate);
        runner.run();

        verify(jdbcTemplate, never()).execute(contains("ALTER TABLE coupons"));
    }

    @Test
    void run_shouldApplyPatchWhenCouponsTableExists() {
        when(jdbcTemplate.queryForObject(contains("table_name = 'coupons'"), eq(Boolean.class)))
                .thenReturn(true);

        CouponSchemaPatchRunner runner = new CouponSchemaPatchRunner(jdbcTemplate);
        runner.run();

        verify(jdbcTemplate).execute(contains("ALTER TABLE coupons ADD COLUMN IF NOT EXISTS scope"));
        verify(jdbcTemplate).execute(contains("ALTER TABLE coupons ADD COLUMN IF NOT EXISTS assigned_user_email"));
        verify(jdbcTemplate).execute(contains("ALTER TABLE coupons ADD COLUMN IF NOT EXISTS assigned_user_id"));
    }
}
